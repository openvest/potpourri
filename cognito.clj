; # Cognito
(ns cognito
  {:nextjournal.clerk/toc true}
  (:require [buddy.hashers :as hashers]
            [buddy.core.hash :as hash]
            [buddy.sign.jws :as jws]
            [buddy.sign.jwt :as jwt]
            [buddy.core.codecs :as codecs
              :refer [to-bytes bytes->str 
                      bytes->hex hex->bytes
                      bytes->b64 b64->bytes]]
            [buddy.core.keys :refer [jwk->public-key]]
            [clojure.string :as str]
            [java-time.api :as jt]
            [cheshire.core :as json]
            [clj-http.client :as client])
  (:import [java.math BigInteger]
           [java.security SecureRandom]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

;;```;; (require '[nextjournal.clerk.viewer :as v])```

;; converting BigInteger to bytes is a common need here 
;; so let's just extend the Protocol in buddy's codecs
(extend-protocol codecs/IByteArray 
  BigInteger
  (-to-bytes [data] (.toByteArray data)))

(defn pad-hex
  "BigInteger here is unsigned but the hex string could result in a negative number
  unless padded with some 0's"
  [^String hex]
  (cond
    (-> (count hex) (mod 2) (= 1)) (str "0" hex)
    (->> (first hex) (str) (str/includes? "89ABCDEFabcdef")) (str "00" hex)
    :else hex))

;; # Cognito endpoints
;; the pool it the highest level of abstraction. i.e. many client applications using one user pool

{:nextjournal.clerk/visibility {:result :hide}}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def user-pool-config
  "requires USER_SRP_AUTH flow (w/ client_secret)"
  (merge {:user-pool-id "us-east-2_INzuX7Ms0"
          :client-id "hubekaq48puviiu82akhrvu0m"
          :client-secret "1dgn40avs48ek0sompk784a1fgv5itnnkstpk7ed1mbs4oilfala"}
         (json/parse-string (slurp "./.ov_password.secret") true))) 

(def user-pool-config-public
  "allows USER_PASSWORD_AUTH flow (no client_secret)"
  (merge {:user-pool-id "us-east-2_INzuX7Ms0"
          :client-id "mcedtgsalss1qln1q4gm84r3s"}
         (json/parse-string (slurp "./.ov_password.secret") true)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Key validation
;; to unsign or validate a token we need to have the public keys assocated with the endpoint
;; first get the config
^{:nextjournal.clerk/visibility {:result :fold}}
(def ^:dynamic *known-token-keys* (atom {}))

(defn get-openid-config
  [user-pool-id]
  (let [region (first (str/split user-pool-id #"_"))]
    (-> (format "https://cognito-idp.%s.amazonaws.com/%s/.well-known/openid-configuration"
                region user-pool-id)
        (client/get {:as :json})
        :body)))

(defn add-user-pool-keys
  "map of :kid (key id from the token header) to an actual public key"
  [user-pool-id]
  (-> (get-openid-config user-pool-id)
      :jwks_uri
      ;; now we have the uri to get the list of authorized keys so go get them
      (client/get {:as :json})
      :body
      :keys
      (->> (into {} (map (juxt :kid jwk->public-key)))
           (swap! *known-token-keys* merge))))

(defn public-key-from-token [token]
                                        ;mostly borrowed from jws/parse-header-
  (let [head-data (-> token 
                      (str/split #"\.")
                      (first)
                      (to-bytes)
                      (b64->bytes)
                      (bytes->str)
                      (json/parse-string true))
        public-key (@*known-token-keys* (:kid head-data))
        alg (keyword (str/lower-case (:alg  head-data)))]
    (when public-key
      [public-key {:alg alg}])))

;; let's actually load some keys for validation
(-> (:user-pool-id user-pool-config)
             (add-user-pool-keys))

;; # Client Application ovdb-public-l
;; for "ClientId" "mcedtgsalss1qln1q4gm84r3s"this is similar to:
;; ```bash
;; aws cognito-idp initiate-auth \
;;     --client-id mcedtgsalss1qln1q4gm84r3s \
;;     --auth-flow USER_PASSWORD_AUTH \
;;     --auth-parameters USERNAME=<user-email>,PASSWORD=<real-password>
;; ```
;; from [blog ref](https://www.devasking.com/issue/im-getting-slightly-different-hmac-signatures-out-of-clojure-and-python)

(defn init-auth-parameters
  "Does not use SRP and does not have client-secret set.
   Therefore, we can use USER_PASSWORD_AUTH flow"
  [{:keys [client-id USERNAME PASSWORD] :as init-params}]
  (assoc init-params :init-auth-params {"AuthFlow" "USER_PASSWORD_AUTH"
                                        "ClientId" client-id   
                                        "AuthParameters" {"USERNAME" USERNAME
                                                          "PASSWORD" PASSWORD}}))

(def init-auth-headers
  {
   "X-Amz-Target" "AWSCognitoIdentityProviderService.InitiateAuth"
   "Content-Type" "application/x-amz-json-1.1" ;; WARNING: do now override with {:as :json}
  })

;; Now we use that password to login and put the tokens into an atom

(defn get-tokens
  [{:keys [user-pool-id init-auth-params]}]
  (-> (format "https://cognito-idp.%s.amazonaws.com/login" (first (str/split user-pool-id #"_" 2)))
      (client/post {:headers init-auth-headers
                    :body (json/generate-string init-auth-params)
                    :content-type "application/x-amz-json-1.1"})
      :body
      (json/parse-string true)
      :AuthenticationResult
                                        ; the token and login endpoints have different key names so lets fix that up
      (clojure.set/rename-keys {:RefreshToken :refresh_token
                                :AccessToken :access_token
                                :IdToken :id_token
                                :TokenType :token_type
                                :ExpiresIn :expires_in})))

(defn unsign-cognito [token]
  (apply jwt/unsign token (public-key-from-token token)))



{:nextjournal.clerk/visibility {:result :show}}
(->  user-pool-config-public
     init-auth-parameters
     get-tokens
     :access_token
     unsign-cognito)

(def tokens (->  user-pool-config-public
                 init-auth-parameters
                 get-tokens))

;; # Client Application ovdb-auth-l
;; This is a different ball of wax because USER_SRP_AUTH requires more work on our side 
;; to avoid sending password on the wire.
;; "ClientId" "hubekaq48puviiu82akhrvu0m"


;; ## Some SRP attempts
;; Questions
;;   * how to aquire cognito username from email and more importantly when to use in over username e.g. "openvest@gmail.com" -> "c16b95a0-4081-70f4-ed0d-2ac9b19bc28d"
;;
;; Note: These params came from `aws_srp.py` from the [python warrant package](https://github.com/capless/warrant)
;; [SO question](https://stackoverflow.com/questions/41526205/implementing-user-srp-auth-with-python-boto3-for-aws-cognito)
;; or this good [blog explanation of SRP](https://medium.com/swlh/what-is-secure-remote-password-srp-protocol-and-how-to-use-it-70e415b94a76)


(defn sign-to-bytes
  "Returns the byte signature of a string with a given key.
  Uses optional {:algorithm xxx} name which deafults to \"HmacSHA256\"."
  [key string & {:keys [algorithm] :or {algorithm "HmacSHA256"}}]
  (let [mac (Mac/getInstance algorithm)
        secretKey (SecretKeySpec. (to-bytes key) (.getAlgorithm mac))]
    (-> (doto mac
          (.init secretKey)
          (.update (to-bytes string)))
        .doFinal)))

(defn sign-to-b64
  "Returns the HMAC SHA256 b64 string signature from a key-string pair."
  [key string]
  (-> (sign-to-bytes key string)
      (bytes->b64)
      (bytes->str)))

(defn get-secret-hash
  [username client-id client-secret]
  (->> (str username client-id)
       (sign-to-b64 client-secret)))


; Public functions

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 


;; ### SRP

;; [Secure Remote Password (SRP)](https://en.wikipedia.org/wiki/Secure_Remote_Password_protocol) is different in that is does not transmit the password to the server side.  Not sure yet if or how this is different from [PKCE](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow-with-proof-key-for-code-exchange-pkce)

;; This flow starts with a submission of SRP_A and a hash of some client context
;;; 
;; (e.g. client_id (which is the app id) and client key (ok that's really the client id but the naming here sucks a little)).

;; This is a multi step process that begins with having to perform 2 calculations:
;;
;;   * one for \`HASH_SECRET\` calculated from username, client-id and client-secret
;;   * for for \`SRP_A\` calculated from mostly random bytes.

;;Things are overly verbose currently as translating between python java and clojure there are many BigInteger, bytes, hex and b64 conversions going on.  (TODO: simplify this)

;; ## Constants used in our calcs

(def g (BigInteger. "2"))
(def big-n
  (-> (str "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
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
      (BigInteger. 16)))

;; k is calculated but it is calculated from constants
;; so let's just skip that and use the resluting BigInteger
;;   * from python: ```hex_to_long(hex_hash('00' + n_hex + '0' + g_hex))```
;;   * from clojure ```(BigInteger. (hash/sha256 (hex->bytes (apply str (map #(bytes->hex (.toByteArray %)) [big-n g])))))```
(def k
  (BigInteger. "37772559067617796309459009502931177628717927509759535181635788491848250400486"))
(def DERIVED-KEY "Caldera Derived Key")

;; some random stuff we will use


(defn modPow
  ([small-x] (modPow g small-x big-n))
  ([g small-x n] (.modPow g small-x big-n)))

(defn init-srp-parameters
  [{:keys [client-id client-secret USERNAME small-a big-a] :as init-params}]
  (let [params (cond-> init-params
                 (and small-a (nil? big-a)) (assoc :big-a (.modPow g small-a big-n))
                 (nil? small-a) (#(let [new-small-a (BigInteger. (* 128 8) (SecureRandom.))]
                                   (assoc % :small-a new-small-a
                                            :big-a   (.modPow g new-small-a big-n)))))
        auth-params (cond-> {"ClientId" client-id
                             "AuthFlow" "USER_SRP_AUTH"
                             "AuthParameters" {"USERNAME" USERNAME
                                               "SRP_A" (bytes->hex (to-bytes (:big-a params)))}}
                      client-secret (assoc-in ["AuthParameters" "SECRET_HASH"]
                                              (get-secret-hash USERNAME client-id client-secret)))]
     (assoc params :init-auth-params auth-params)))

(defn get-srp-challenge
  [{:keys [user-pool-id init-auth-params] :as init-config}]
  (-> (format "https://cognito-idp.%s.amazonaws.com/login" (first (str/split user-pool-id #"_" 2)))
      (client/post {:headers init-auth-headers
                    :body (json/generate-string init-auth-params)})
      :body
      (json/parse-string true)
      (->> (assoc init-config :auth-challenge))))

; ### initiate challenge
{:nextjournal.clerk/visibility {:result :show}}

;; full request that returns a challenge (i.e. with SRP_B
(-> (init-srp-parameters user-pool-config)
    (get-srp-challenge))

;; ### process challenge


(require '[clojure.pprint :refer [pprint]])
;(pprint auth-challenge)

(defn big-integer-to-hex
  [^BigInteger big-integer]
  (bytes->hex (to-bytes big-integer)))

(defn calculate-u
  [big-a big-b]
  ;; TODO: sort out "0" padding at the front
  (-> (str (big-integer-to-hex big-a) (big-integer-to-hex big-b))
      (hex->bytes)
      (hash/sha256)
      (bytes->hex)
      (BigInteger. 16)))


(defn compute-hkdf [^bytes s  ^bytes u]
  (let [mac (Mac/getInstance "HmacSHA256") ;
        prk (-> (doto mac
                  (.init (SecretKeySpec. u "SHA256"))
                  (.update s))
                (.doFinal))
        info-bits (to-bytes (str "Caldera Derived Key" (char 1)))
        hmac-hash (-> (doto mac
                        (.reset)
                        (.init (SecretKeySpec. prk "SHA256"))
                        (.update info-bits))
                      (.doFinal))]
    (byte-array (take 16 hmac-hash))))

(defn get-password-authentication-key
  [pool-id user-name password small-a big-a big-b salt]
  (let [u-value (calculate-u big-a big-b)
        pool (second (str/split pool-id #"_" 2))
        username-password (-> (format "%s%s:%s" pool user-name password)
                              (hash/sha256)
                              (bytes->hex))
        x-value (-> (pad-hex salt)
                    (str username-password)
                    (hex->bytes)
                    (hash/sha256)
                    (bytes->hex) ; TODO: do we need another (pad-hex) here?
                    (BigInteger. 16))
        int-val2 (->> (modPow x-value)
                      (.multiply k)
                      (.subtract big-b))
        s-value (.modPow
                 int-val2
                 (-> (.multiply u-value x-value)
                     (.add small-a))
                 big-n)]
    #_(pprint {'u-value u-value
             'username-password username-password
             'x-value x-value
             'g-mod-pos-xn g-mod-pow-xn
             'int-val2 int-val2
             's-value s-value}) 
    (compute-hkdf
     (hex->bytes (pad-hex (bytes->hex (to-bytes s-value))))
     (hex->bytes (pad-hex (bytes->hex (to-bytes u-value)))))))

(defn- get-timestamp
  []
  (jt/format "EEE MMM d HH:mm:ss z yyyy" (jt/zoned-date-time (jt/instant) "UTC")))

(defn process-challenge
  "process a challenge"
  [{:keys [user-pool-id small-a big-a client-id client-secret PASSWORD] :as config}]
  ;(print [user-pool-id small-a big-a client-id client-secret PASSWORD])
  (let [{salt-hex :SALT
         big-b-hex :SRP_B
         secret-block-b64 :SECRET_BLOCK
         user-id-for-srp :USER_ID_FOR_SRP
         challenge-username :USERNAME} (get-in config [:auth-challenge :ChallengeParameters])
        big-b (BigInteger. big-b-hex 16)
        u-value (calculate-u big-a big-b)
        secret-block-bytes (-> secret-block-b64
                               (to-bytes)
                               (b64->bytes))
        ;; hardcode this for now
        timestamp (get-timestamp)
        ;timestamp "Wed Jun 7 20:04:25 UTC 2023"
        pool-name (second (str/split user-pool-id #"_" 2)) ;; region_poolname => poolname
        message  (->> [pool-name challenge-username secret-block-bytes timestamp]
                      (mapcat to-bytes)
                      (byte-array))
        hkdf   (get-password-authentication-key
                user-pool-id 
                user-id-for-srp
                PASSWORD
                small-a 
                big-a 
                big-b
                salt-hex)
        response-params (cond-> {"TIMESTAMP" timestamp
                                 "USERNAME" challenge-username
                                 "PASSWORD_CLAIM_SECRET_BLOCK" secret-block-b64
                                 "PASSWORD_CLAIM_SIGNATURE" (sign-to-b64 hkdf message)}
                          client-secret (assoc "SECRET_HASH" (get-secret-hash
                                                                  user-id-for-srp
                                                                  client-id
                                                                  client-secret)))]    
    (assoc config :challenge-response {"ClientId" client-id
                                       "ChallengeName" "PASSWORD_VERIFIER"
                                       "ChallengeResponses" response-params})))

(def challenge-response-headers
  {"X-Amz-Target" "AWSCognitoIdentityProviderService.RespondToAuthChallenge",
   "Content-Type" "application/x-amz-json-1.1"})

;; Add "SECRET_HASH" to this if there is aa client-secret
(defn submit-challenge-response
  [{:keys [user-pool-id challenge-response]}]
 (try
   (client/post "https://cognito-idp.us-east-2.amazonaws.com/"
                {:headers challenge-response-headers
                 :body (json/generate-string challenge-response)})
   (catch Exception e (-> e (Throwable->map)
                          :data
                          ((juxt :status #(json/parse-string (:body %))))))))

;; ## Without client-secret


;; ### process challenge

;; Add "SECRET_HASH" to this if there is aa client-secret

;; # process captive challenge
(def challenge-parameters {
  :SALT "67c4a32d53ce3ce23d205a7bc174fa67",
  :SECRET_BLOCK "EIPIsRuoGCCM7iiY9519E7zV9ZCtzG0S6KDFZuoPmiiPCma2/+rMtSISi3cvE5XXemEFAd0txaFXqux4ghpPpXahKAjb02o+uggbrS69zeReQxCRucx6mE23qKPazCbAEGBIwtNPoqjxXcdy7RXSsOVKgzDkl0XMwTFFl8aZIN2NZdn50VPhFaGVmXm3uideFqZnLEFJVcHIrnAz7gjIz1bvVtG/uxPXxrForU8paJfQU6vFnI8c/ODEAuXQPtvzoCyu10M/wjMww7zB0hQktXAQUYILabvbfW9fFeySBis1+LUrah0w1zR2EuvWE9bCioGEjshCX+Vly6TxC62H0QALRCErluHcTMSCN36KtJLACwjWH1FcBV9blzaOWTDxMr2l2FbU0yiICc/agGyADFG1iaoJ50yRAm3lFXqxEFwlYAok1zsqTv8adX7cLqjq/kwiDZkGBNvhC7brVkn1wIRzI2YRWvqSfxAXXwmW6K8jUlZpA+P8/OauLrS6A929Y4B3jnnLizmbIPsQOAEIB7ylh9oC/8IhIP+sxMeJjHkyR7tY08PkCef+vEPGiS1pC4GpjYb+HT48ZjZV+5L2JQjHgDRUM4WpdeT1ir2ve8v/7i8QisaW3KIwMQ6i3tmd4O5nlRnFUIXLitXDRGotJctYqrT5mrGzxktupu3UKxVG14qXG4xIMZrre1LIm9z2V278+xALVDp7RIn5I/1jr0ZGovDij83Ws2kddiQdNSMZJgu+AZ2TIo2XGNgBzApIL6WGfdVn44rYI8AMarpszcDhBrf7j0pZkza9m29lBXzv4hUQFHQA/mP07ACYuoUpfMUJl3GDvrajlSZmTGFivo1lk16oGtLNrlGzyYiphQ5Igf31+0AGQz0E0ghfHeSr/1ElobKqLeCjoD8oa0V71yPV5OcxFaIWSXmRPlVyqP4DD7eTtwnbABeoQbmCF87oZJ/8tRPVOtviJczQFNUg5/wB3OdL4+Gs4XMLe5cS+yk9HZEGd/8OxEdL0VfT0ernRWEwyT4lrZHEEuAfbcomGtBvai3xW0zBXGZ2P5dAHOTISV6yLLzJm54EH/w8cC5xXuALcSukFds+7RcTYusFufWdKuHfr2s2WA7K5OO4XVT4BlofnbvYx9FXukoXVB6rfiLPfREUQrsXJvdaZuBdz8eVOmCuCLD93JvNmTInk/k2jYvpySmgLqLlxlKCsY9HmLTIwpx+97UbBZfsSfMmx6fhdeVGK89ydQKMd0YR6egb4gN153F04sTgaF1+27SeOg5neCAWMwuho99+cXv57gRaTxpFGJTITwjZHGl2Os/cKAe6ZxwPpWUpKBtYNqPSnxl9yNQmVpr2MybI4ouDSOYEZKaAUjWwTUJAmzVifsrz+jfMky2/m/LpasmghCMdlUCofUD2UYuuIY7PA9mOZpQ9JqRqPyQMCQPntALjbVwy5cxfkzCPyIk92LGB/0iteM3dTfOiSYwg31HUj9V16wDJoYNxI82kgwOypN3NsJUGJU2tUa16Vsk/LiHUYZxut/Ym7jUE7iVEzDamOB4ndwfRt2mP1erB9cxwjT/XiwJ7Jy3bqF94XOX9y/oCh64y3abeAB9nfYoKsYhC/Lxf0KaaoyszaqirgTjfVAuZBHpBmmxg9fim2QX5hZXVjw+3pXl+5BHDrSLm9aT5JZ8PTUEM8TnyuLbYxV5iE8l1GrWn16I1y7ZYaeilYA==",
  :USER_ID_FOR_SRP "c16b95a0-4081-70f4-ed0d-2ac9b19bc28d",
  :USERNAME "c16b95a0-4081-70f4-ed0d-2ac9b19bc28d",
  :SRP_B "cbb9f672764ff8800b2854b307d213ce979989d1c3cb2f34a14ea2141d2b3332d4ae7107e74080dc9e1d0cbfda438e9e550f3d4c34443f12b05e645bf3e5b9cd873e7ef3fbb2ac2405b4682bc6bcdb1644c9ea6861be1244ff4efc7ae3530984d08ca53fd1eef46a2a953df91c122118bf58b394613798c385d2ae2cca1155fda48d7c8373604ac672f798c9b938d92b31b92b0bde8da94ce42c8f265698c5e5e4264ef7f641aec7256d9bbd74cdf35ea374d2d9cc1eabb0d79871fabdde4314612ac6eb9685531cd5de94bce738d9e18fe572c060a56a552a8013874c5454793d3b3faface581fdc01b7d2ca5a52289b9dfce76712aecdffded1abc4a79611a8ff8b50550939d7128a47d7b01c07c6224260724f9a8bf2c5790764d2baf6f547d73ca073ea3e15a819e0022dcdce9e8c49aaefd74bacc1c92d9779fdbac44cb28cda560f26ce2b977eef78ada7eaf1015ec5dafc35e88e49584cc526fe742a0948069679df7e392ee48615a2c7e1305574252a3ea81b43218d9217660634a56"
  })


(def small-a (BigInteger. "52284328694614363893254713603699386001743535533625001204058829857661791776897624351714718010676395461421997204279005241789676752229614122041208761019788050612691423116244063932876018325834519442551416191892913493777665576100860570447400409780110456950062725767009935336683127011563674556955285149953096752559"))
;(process-challenge small-a user-pool-id challenge-parameters)

