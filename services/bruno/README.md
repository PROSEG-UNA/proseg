# Bruno collections

Set `auth_identifier` and `auth_password` in a local Bruno environment before sending login requests. Do not commit populated values; local `environments/*.local.yml` files are ignored.

Credentials previously hard-coded in collection login requests must be treated as exposed and revoked or rotated, even though they have been removed from the current files.
