# generate-jwt.py
#
# Follow the setup steps in
#       https://auth0.com/blog/how-to-handle-jwt-in-python/
# prior to running this.
#
# How to run this:  python3 ./generate-jwt.py
#
# Be sure to change my_client_id, below, to be your client-id string, as given to you by the ReportStream team.
#
import jwt
import datetime
import time
import math
import uuid
from cryptography.hazmat.primitives import serialization

#
# REPLACE THESE WITH YOUR CLIENT-ID AND KEYPAIR FILE
#
my_client_id = "health-o-matic-labs"
my_rsa_keypair_file = "./my-rsa-keypair.pem"

#
# Step 1
#
print(
f'''
STEP 1:  Prior to submission, send your public key to ReportStream

This program assumes there is an RSA keypair in the file {my_rsa_keypair_file}

This keypair can be generated by running these commands in that directory:
	openssl genrsa -out {my_rsa_keypair_file} 2048
	openssl rsa -in {my_rsa_keypair_file} -outform PEM -pubout -out my-rsa-public-key.pem

Then send the public key file to the ReportStream team.

''')

#
# Step 2
#

private_key = open(my_rsa_keypair_file, 'r').read()
key = serialization.load_pem_private_key(private_key.encode(), password=None)

now = math.floor(datetime.datetime.timestamp(datetime.datetime.now()))

header_data = { "kid": f"{my_client_id}.default"}

payload_data = {
    "iss": f"{my_client_id}.default",
    "sub": f"{my_client_id}.default",
    "aud": "staging.prime.cdc.gov",
    "exp": now + 300,                                # Expire in 5 minutes
    "jti": str(uuid.uuid4())
}

print(f'The payload is {payload_data}\n')

token = jwt.encode(
    payload=payload_data,
    key=key,
    algorithm='RS256',
    headers=header_data
)

print(
f'''
STEP 2:  At the Time of Submission to ReportStream, generate a signed JWT using your private key

{token}

''')

#
# Examples of Steps 3 and 4
# 

print(
f'''
STEP 3:  Send that signed JWT to ReportStream, to get a temporary bearer token

EXAMPLE: Here is an example curl call to request a submission token valid for the next 5 minutes, using this JWT.
This assumes you have already given your public key to ReportStream.

curl -X POST -H "content-length:0" "https://staging.prime.cdc.gov/api/token?scope={my_client_id}.default.report&grant_type=client_credentials&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&client_assertion={token}"

If its working, you'll get back a response like this:

{{"sub":"{my_client_id}.default.report_90ffad77-9b47-448f-ab51-9b40c856d878","access_token":"ACCESS-TOKEN-STRING-HERE","token_type":"bearer","expires_in":300,"expires_at_seconds":1660744830,"scope":"{my_client_id}.default.report"}}



STEP 4: Submit data to ReportStream using the bearer token

EXAMPLE: Here is an example submitting an HL7 2.5.1 payload:

curl -X POST -H "authorization:bearer ACCESS-TOKEN-STRING-HERE" -H "client:{my_client_id}"  -H "content-type:application/hl7-v2" --data-binary "@./my-nonPII-data.hl7" "https://staging.prime.cdc.gov/api/waters"



''')