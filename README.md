# LFTP

Send and receive big file by udp

## Usage

```bash
Usage: lftp [-hV] [COMMAND]
Send and receive big file by udp.
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  server  Send and receive big file by udp.
  lsend   Send file to server.
  lget    get file from server.
  list    list files in server.

Usage: lftp server [-hV] [-c=<clientCount>] [-d=<dir>] [-p=<port>]
                   [-s=<portPoolStart>]
Send and receive big file by udp.
  -c, --client=<clientCount>
                      The number of clients at the same time.
  -d, --dir=<dir>     Server dir dir.
  -h, --help          Show this help message and exit.
  -p, --port=<port>   Server listen port.
  -s, --start=<portPoolStart>
                      Start port pool
  -V, --version       Print version information and exit.
  
Usage: lftp lsend [-hV] [-c=<controlPort>] [-p=<sendPort>] [-s=<server>]
                  [<files>...]
Send file to server.
      [<files>...]        file path
  -c, --control=<controlPort>
                          Control port.
  -h, --help              Show this help message and exit.
  -p, --send=<sendPort>   Send port.
  -s, --server=<server>   Server location.
  -V, --version           Print version information and exit.

Usage: lftp lget [-hV] [-d=<dir>] [-p=<controlPort>] [-s=<server>] [<files>...]
get file from server.
      [<files>...]           file path
  -d, --dir=<dir>            Save file dir.
  -h, --help                 Show this help message and exit.
  -p, --port=<controlPort>   Port.
  -s, --server=<server>      Server location.
  -V, --version              Print version information and exit.

Usage: lftp list [-hV] [-s=<server>]
list files in server.
  -h, --help              Show this help message and exit.
  -s, --server=<server>   Server location.
  -V, --version           Print version information and exit.
```

### Example

```bash
# Run a server
$ LFTP server -p=3000 -d=./data
# Send a file
$ LFTP lsend -s=127.0.0.1:3000 ./data/test.txt
# Get a file
$ LFTP lget -s=127.0.0.1:3000 -d ./data text.txt
# List file
$ LFTP list -s=127.0.0.1:3000
```

