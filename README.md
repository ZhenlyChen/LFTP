# LFTP

Send and receive big file by udp

## Usage

```bash
Usage: LFTP [-hV] [COMMAND]
Send and receive big file by udp.
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  server  Send and receive big file by udp.
  lsend   Send file to server.
  lget    get file from server.
  list    list files in server.

Usage: LFTP server [-hV] [-d=<data>] [-p=<port>]
Send and receive big file by udp.
  -d, --data=<data>   Server data dir.
  -h, --help          Show this help message and exit.
  -p, --port=<port>   Server listen port.
  -V, --version       Print version information and exit.

Usage: LFTP lget [-hV] [-s=<server>] [<file>...]
get file from server.
      [<file>...]         file path
  -h, --help              Show this help message and exit.
  -s, --server=<server>   Server location.
  -V, --version           Print version information and exit.

Usage: LFTP lsend [-hV] [-s=<server>] [<file>...]
Send file to server.
      [<file>...]         file path
  -h, --help              Show this help message and exit.
  -s, --server=<server>   Server location.
  -V, --version           Print version information and exit.

Usage: LFTP list [-hV] [-s=<server>]
list files in server.
  -h, --help              Show this help message and exit.
  -s, --server=<server>   Server location.
  -V, --version           Print version information and exit.
```

### Example

```bash
# Run a server
$ LFTP server -p=3000 -d=./data/
# Send a file
$ LFTP lsend -s=127.0.0.1:3000 ./test.txt
# Get a file
$ LFTP lget -s=127.0.0.1:3000 ./text.txt
# List file
$ LFTP list -s=127.0.0.1:3000
```
