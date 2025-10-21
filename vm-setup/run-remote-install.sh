#!/bin/bash
ssh -i /app/vm-setup/id_rsa root@91.98.80.35 << 'REMOTE_EOF'
cd /root/vm-setup-files
nohup bash install-vm.sh > /root/install-output.log 2>&1 < /dev/null &
echo $! > /root/install-vm.pid
echo "Installation started with PID: $(cat /root/install-vm.pid)"
echo "Monitor with: tail -f /root/install-output.log"
REMOTE_EOF
