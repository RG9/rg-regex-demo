#!/bin/bash

# Test of ReDos for Python
# https://owasp.org/www-community/attacks/Regular_expression_Denial_of_Service_-_ReDoS
# input length: 25 => time: ~0,9s
# input length: 30 => time: ~28,4s

INPUT_LENGTH=${1:-30} # default 30

python3 << END
import re
import time

pattern = re.compile(r'^(a+)+$')
test_string = "a" * $INPUT_LENGTH + "!"

start_time = time.time()
match = pattern.match(test_string)
end_time = time.time()

print(f"Time: {end_time - start_time} s")
END
