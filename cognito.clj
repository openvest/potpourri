; # Requirements

(require '[clojure.tools.deps.alpha.repl :refer [add-libs]])
(add-libs '{buddy/buddy-auth             {:mvn/version "3.0.323"}
            buddy/buddy-hashers          {:mvn/version "1.8.158"}})
(ns clerk-buddy
  (:require [buddy.hashers :as hashers]
            [buddy.auth :as auth]
            [buddy.auth.backends.token :as token]
            [buddy.sign.jws :as jws]
            [buddy.sign.jwt :as jwt]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :as mw]
            [buddy.core.codecs :as codecs]
            [buddy.core.codecs.base64 :as b64]  ;; deprecated use codecs equiv
            [buddy.core.keys :refer [jwk->public-key]]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clj-http.client :as client])
  (:import [java.security MessageDigest]
           [java.math BigInteger]))


;; # Cognito endpoints
;; the pool it the highest level of abstraction. i.e. many client applications using one user pool
(def user-pool-id "us-east-2_INzuX7Ms0")

(def openid-config
  (-> "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_INzuX7Ms0/.well-known/openid-configuration"
      (client/get {:as :json})
      :body))

(def openid-token-endpoint (:token_endpoint openid-config))

(def openid-authorization-endpoint (:authorization_endpoint openid-config))

(def openid-key-lookup
  "map of :kid (key id from the token header) to an actual public key"
  (-> (client/get "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_INzuX7Ms0/.well-known/openid-configuration"
                  {:as :json})
      :body
      :jwks_uri
      ;; now we have the uri to get the list of authorized keys so go get them
      (client/get {:as :json})
      :body
      :keys
      (->> (into {} (map (juxt :kid jwk->public-key))))))

(defn public-key-from-token [token]
  ;mostly borrowed from jws/parse-header-
  (let [head-data (-> token 
                      (str/split #"\.")
                      (first)
                      (b64/decode)
                      (codecs/bytes->str)
                      (json/parse-string true))
        public-key (openid-key-lookup (:kid head-data))
        alg (keyword (str/lower-case (:alg  head-data)))]
    [public-key {:alg alg}]))


;; # Client Application ovdb-public-l
;; "ClientId" "mcedtgsalss1qln1q4gm84r3s"
(def ovdb-public-l
  "Does not use SRP and does not have client-secret set.
   Therefore, we can use USER_PASSWORD_AUTH flow"
  {
    "AuthParameters" {
        "USERNAME" "openvest@gmail.com",
        "PASSWORD" "Openvest_1415"
        },
    "AuthFlow" "USER_PASSWORD_AUTH",
    "ClientId" "mcedtgsalss1qln1q4gm84r3s"
   })
;; Now we use that password to login and put the tokens into an atom
(def tokens
  (-> (client/post "https://cognito-idp.us-east-2.amazonaws.com/login"
                   {:headers auth-headers
                    :body (json/generate-string ovdb-public-l)
                    :content-type "application/x-amz-json-1.1"})
      :body
      (json/parse-string true)
      :AuthenticationResult
      ; the token and login endpoints have different key names so lets fix that up
      (clojure.set/rename-keys {:RefreshToken :refresh_token
                                :AccessToken :access_token
                                :IdToken :id_token
                                :TokenType :token_type
                                :ExpiresIn :expires_in})
      atom))

(defn unsign-cognito [token]
  (apply jwt/unsign token (public-key-from-token token)))

(unsign-cognito (:id_token @tokens))




;; # Client Application ovdb-auth-l
;; This is a different ball of wax because USER_SRP_AUTH requires more work on our side 
;; to avoid sending password on the wire.
;; "ClientId" "hubekaq48puviiu82akhrvu0m"

(def ov-auth-l
  "uses SRP and has a client-secret set
   must use USER_SRP_AUTH flow"   
  {    
    "AuthFlow" "USER_SRP_AUTH",
    "ClientId" "hubekaq48puviiu82akhrvu0m"
    "ClientSecret" "1dgn40avs48ek0sompk784a1fgv5itnnkstpk7ed1mbs4oilfala"
   })

(def auth-headers
  {
   "X-Amz-Target" "AWSCognitoIdentityProviderService.InitiateAuth"
   "Content-Type" "application/x-amz-json-1.1" ;; see WARNING below
  })

(try
  (-> (client/post "https://cognito-idp.us-east-2.amazonaws.com/login"
                  {:headers auth-headers
                   :body (json/generate-string ov-auth-l)
                   :content-type "application/x-amz-json-1.1"})
     :body
     (json/parse-string true))
  (catch Exception e (print e)))


;; ## Some SRP attempts
;; Questions
;;   * how to aquire cognito username from email e.g. "openvest@gmail.com" -> "c16b95a0-4081-70f4-ed0d-2ac9b19bc28d"
;;   * how to generate SECRET_HASH  (hash username+password+client_secret)
;;   * how to gneerate SRP_A (just random??)
;;
;; Note: These params came from `aws_srp.py` from the [python warrant package](https://github.com/capless/warrant)
;; [SO question](https://stackoverflow.com/questions/41526205/implementing-user-srp-auth-with-python-boto3-for-aws-cognito)
;; or this good [blog explanation of SRP](https://medium.com/swlh/what-is-secure-remote-password-srp-protocol-and-how-to-use-it-70e415b94a76)

(def ov-srp-l
  {"AuthFlow" "USER_SRP_AUTH"
   "ClientId" "hubekaq48puviiu82akhrvu0m"
   "AuthParameters" {"USERNAME" "c16b95a0-4081-70f4-ed0d-2ac9b19bc28d", ; "openvest@gmail.com"
                     "SRP_A" "dd4940cd99795a4b4aac46adfa20220851d5a5d48821a7fb59687b53242b5a43ae2089649d5b89c3804e25e7ba44c0e66a7fe2fc426db84ed6fce772ca21ffd395fbb6bfec2031e04cbaea1a4f745d1bcd48215b4a6153a606013544de3910e6fa4c089c785f40cf2e93ce426adbcc93479b209fccb32d49d9cf6999669c2d6d10d65c8933d489991d42c82c9003dcf66a529c9403051c36c846aa87b089564e26795d601c1ec40dd926d830cc002732e4aba4784af2f24c4bf3abbfa95e3d2b6f646e3a16ebbc07ffb7e4cc2d4c3e0e320d125100ef4a88716902ca1c8a11f2aa2a31f99aef1ae927e60b81b7bc0a9fb53abe5d198340abc5e9d601a0d2a11101def90cec50028c92540a96fad2be868e3036d7dab7819087b98606712e2889a96eb7802fc179a57992e763af5e5b5267732e6f66837c205adab8d20b95138565fe4819fe6f8c8cbedc4be464606d7f37885714f9ce2462aa931af939050ba67c1e358f3ac6c4aa6fffba0a70a065a4d14e41d0d1ce480385f71b08109309ec",
                     "SECRET_HASH" "KdhHHXQhDnHPIrO6wiX2KRwtQr2DL2k56O7bUQ1k7HY="}}
  )

(-> (client/post "https://cognito-idp.us-east-2.amazonaws.com/login"
                 {:headers auth-headers
                  :body (json/generate-string ov-srp-l)
                  :content-type "application/x-amz-json-1.1"})
    :body
    (json/parse-string true))


;; What's up with that becuase this works in python
(try 
 (client/post "https://ovdb-demo1.auth.us-east-2.amazoncognito.com/oauth2/token",
              {:form-params {
                             "grant_type" "authorization_code",
                             "code" "ae42c7a1-f01a-4b64-9ee2-9324f8722c15",
                             "client_id" "hubekaq48puviiu82akhrvu0m",
                             "redirect_uri" "http://ovdb.openvest.com/schema"
                             }
               :headers {"Authorization" "Basic aHViZWthcTQ4cHV2aWl1ODJha2hydnUwbToxZGduNDBhdnM0OGVrMHNvbXBrNzg0YTFmZ3Y1aXRubmtzdHBrN2VkMW1iczRvaWxmYWxh"
                         "Content-Type" "application/x-www-form-urlencoded"}
               :as :json})
 (catch Exception e (print e)))


;; aws cognito-idb sign-up --clientid ... --username ... --password
;; aws cognito-idb initate-auth --clientid --auth-flow USER_PASSWORD_AUTH USERNAME=openvest@gmail.com,PASSWORD=Openvest_1415
;; aws cognito-idp initiate-auth --client-id mcedtgsalss1qln1q4gm84r3s --auth-flow USER_PASSWORD_AUTH --auth-parameters USERNAME=openvest@gmail.com,PASSWORD=Openvest_1415

;; from https://www.devasking.com/issue/im-getting-slightly-different-hmac-signatures-out-of-clojure-and-python
(import '[javax.crypto Mac]
        '[javax.crypto.spec SecretKeySpec])

(def algorithm "HmacSHA256")

(defn return-signing-key [key mac]
  "Get an hmac key from the raw key bytes given some 'mac' algorithm.
  Known 'mac' options: HmacSHA1"
    (SecretKeySpec. (.getBytes key) (.getAlgorithm mac)))

(defn sign-to-bytes [key string]
  "Returns the byte signature of a string with a given key, using a SHA1 HMAC."
  (let [mac (Mac/getInstance algorithm)
    secretKey (return-signing-key key mac)]
    (-> (doto mac
      (.init secretKey)
      (.update (.getBytes string)))
    .doFinal)))

; Formatting
(defn bytes-to-hexstring [bytes]
  "Convert bytes to a String."
  (apply str (map #(format "%x" %) bytes)))

(defn bytes-to-b64 [bytes]
  (apply str (map char  (b64/encode  bytes))))

; Public functions
(defn sign-to-hexstring [key string]
  "Returns the HMAC SHA1 hex string signature from a key-string pair."
  (bytes-to-hexstring (sign-to-bytes key string)))

(sign-to-hexstring "boo hoo" "bow wow")

(defn sign-to-b64 [key string]
  "Returns the HMAC SHA1 hex string signature from a key-string pair."
  (bytes-to-b64 (sign-to-bytes key string)))

(sign-to-b64 "boo hoo" "bow wow")

(defn get-secret-hash
 [username client-id client-secret]
  (let [secret client-secret
        message (str username client-id)]
    (sign-to-b64 secret message)))

(get-secret-hash
 "c16b95a0-4081-70f4-ed0d-2ac9b19bc28d"
 "hubekaq48puviiu82akhrvu0m"
 "1dgn40avs48ek0sompk784a1fgv5itnnkstpk7ed1mbs4oilfala")
;; => "KdhHHXQhDnHPIrO6wiX2KRwtQr2DL2k56O7bUQ1k7HY="


(str "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
     "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
     "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
     "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
     "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
     "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
     "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
     "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
     "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
     "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
     "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64"
     "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7"
     "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B"
     "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C"
     "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31"
     "43DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF")
