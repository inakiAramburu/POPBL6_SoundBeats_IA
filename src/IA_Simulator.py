import sys
import random

def get_random_cardio_disease():
    diseases = [
        "murmur",
        "extraestole",
        "extrahls",
        "artifact",
        "normal"
    ]
    return random.choice(diseases)

# Read the input string from command-line argument
input_string = sys.argv[1]

# Generate a random cardiovascular disease
result = get_random_cardio_disease()

# Print the result for Java to read
print(result)