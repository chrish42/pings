"""Very simple storage server. Gets JSON messages via Zeromq and appends
them to a file. A new file is used every hour.

We don't call sync after every message to get the best bandwidth from the
disk. Given that a new file is used every hour, if there is a problem, we
lose only that hour of data. This is reliable enough for our purposes. The
files are also created on-demand, so if there is no activity for a while,
we don't get a ton of empty files.

The files are stored in a different directory for each day. The date and
hours are in the UTC timezone, to avoid problems when changing to or from
daylight saving time.

To use more than one storage server on a single computer, pass them
different root directories."""

import sys, os, datetime, errno, json, zmq

class OnDemandFile:
    """Creates a new file on-demand every hour. Files are stored in
    directories named for the UTC date."""
    def __init__(self, root_dir='.'):
        self.root_dir = root_dir
        self.f = None
        self.current_date = None
        self.current_hour = None

    def _get_file(self):
        dt = datetime.datetime.utcnow()
        date = dt.date()
        hour = dt.hour
        
        if date != self.current_date:
            try:
                os.mkdir(date.isoformat())
            except OSError, e:
                if e.errno == errno.EEXIST:
                    # Ignore error when directory already exists. This can
                    # happen when the storage server is restarted.
                    pass
                else:
                    raise
            self.current_date = date
            
        if hour != self.current_hour:
            if self.f is not None:
                f.close()
            self.f = open(os.path.join(self.root_dir,
                                       self.current_date.isoformat(),
                                       '%d.data' % hour), 'wt')
            self.current_hour = hour

        return self.f
        
    def write_msg(self, msg):
        """Appends a message to the file. A different file is used every
        hour. Messages are separated by a newline."""
        f = self._get_file()
        f.write(msg)
        f.write('\n')


def main():
    if len(sys.argv) > 2:
        print sys.stderr, "Usage: storage_server [directory]"
        sys.exit(1)

    root_dir = sys.argv[1] if len(sys.argv) == 2 else '.'
    f = OnDemandFile(root_dir)
    
    zmq_context = zmq.Context()
    zmq_socket = zmq_context.socket(zmq.PULL)
    zmq_socket.bind('tcp://127.0.0.1:5000')
    
    while True:
        msg = zmq_socket.recv_json()
        f.write_msg(json.dumps(msg))
