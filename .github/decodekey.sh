#!/usr/bin/env sh

echo $PGP_SECRET | base64 --decode | gpg --import --no-tty --batch --yes

echo "allow-loopback-pinentry" >>~/.gnupg/gpg-agent.conf
echo "pinentry-mode loopback" >>~/.gnupg/gpg.conf

gpg-connect-agent reloadagent /bye
