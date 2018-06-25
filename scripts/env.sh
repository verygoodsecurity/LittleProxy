#!/bin/bash

set -x

mkdir -p ~/.aws
touch ~/.aws/credentials

echo "
[vgs-dev]
region = us-west-2
role_arn = arn:aws:iam::883127560329:role/StageDeploy
source_profile = default
" | tee -a ~/.aws/credentials

