import requests

requests.post("https://cognito-idp.us-east-2.amazonaws.com/login",
   json={
        "AuthParameters": {
            "USERNAME": "openvest@gmail.com",
            "PASSWORD": "Openvest_1415"
            },
        "AuthFlow": "USER_PASSWORD_AUTH",
        "ClientId": "mcedtgsalss1qln1q4gm84r3s"
   },
   headers={"X-Amz-Target": "AWSCognitoIdentityProviderService.InitiateAuth",
            "Content-Type": "application/x-amz-json-1.1"}).json()



import  warrant

# how do i get the client_id (e.g. c16b95.....28d ) from the email?h
a = warrant.aws_srp.AWSSRP("c16b95a0-4081-70f4-ed0d-2ac9b19bc28d", "Openvest_1415", "us-east-2_INzuX7Ms0", "hubekaq48puviiu82akhrvu0m",
                            pool_region="us-east-2", client_secret="1dgn40avs48ek0sompk784a1fgv5itnnkstpk7ed1mbs4oilfala")

# used in clj code:
# a.get_auth_params()

"""
# not sure if this USERNAME is right or if we need the cognito user

aws cognito-idp initiate-auth --auth-flow USER_SRP_AUTH  --client-id hubekaq48puviiu82akhrvu0m --auth-parameters "USERNAME=openvest@gmail.com,SRP_A=ed196121e9c11a3ec252db9b29ab25e36566d75a32fa4b2beedf0ec291b7a12076daf3340f0d4e2603ad2d83c973f6ec3e892826d8fea92699958ce9c89117022d68def36a06724e763cd00f854b595d23dcf72d90a578cdeec37cc6791125e99c7b9c2fbd673cbcd9796694340b4dbc7758e8839c3405d4af76fcd6fcc4d4625eb02021d578e8db93be436112a9889a2ce0a656f544bb920d776ff99b8b1e14f1a3150afc9b569e0f246f3d1c9d3377360ffb7a35e9e8beb198c457a1a991aaaccf2e75698cfd063808863261e51360f28d5b7684c9b684323216da7c493257fd027a35289eb20e79b128f8dcea973f9308b6906bede8961bcc3b7cd652d1d687f0039a24ca65ce9ccff58749097260ee6e73d1cd0272905652c5446b267497e19966b69de82ec273b11bcc81787b551796a2717d23ffc4c34ba5fadc9f9f4743343f6eacf93d82c642c4d39b3f0d5557e73256731e05bc2f13aaae17e47cc9f896eeefb2,SECRET_HASH=Tds2PJ7XWkWkX5jjYvk+mMqxi/JDRtjUMROCfFgNgkY="
"""
