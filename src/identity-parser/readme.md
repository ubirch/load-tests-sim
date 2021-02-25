###Description
This is a script to parse an input file with the format
```
{"UUID":"<uuid1>","deviceCredentials":{"password":"<pw1>"}}
{"UUID":"<uuid2>","deviceCredentials":{"password":"<pw2>"}}
{"UUID":"<uuid3>","deviceCredentials":{"password":"<pw3>"}}
...
```
into an output file `identities,json`:
```json
[
  {
    "role": "some-role",
    "uuid": "uuid1",
    "password": "pw1"
  },
  {
    "role": "some-role",
    "uuid": "uuid2",
    "password": "pw2"
  },
  {
    "role": "some-role",
    "uuid": "uuid3",
    "password": "pw3"
  },
  ...
]
```

###Usage:
```
python3 ./identity_parser.py <file-name>
```