#!/bin/bash
ssh -fNL 9000:127.0.0.1:9000 wydev
ssh -fNL 9001:127.0.0.1:9001 wydev
ssh -fNL 3306:127.0.0.1:3306 wydev
ssh -fNL 5672:127.0.0.1:5672 wydev
ssh -fNL 15672:127.0.0.1:15672 wydev
ssh -fNL 6379:127.0.0.1:6379 wydev
ssh -fNL 9091:127.0.0.1:9091 wydev
ssh -fNL 19530:127.0.0.1:19530 wydev
ssh -fNL 3000:127.0.0.1:3000 wydev
ssh -fNL 5001:127.0.0.1:5001 wydev
