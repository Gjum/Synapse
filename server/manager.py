#!/usr/bin/env python3
# Copyright (C) 2020 Axon Contributors
# This script is licensed under the terms of the ISC License
# screw the GPL, post was made by ISC/BSD gang
"""Manager for Axon/Synapse server configuration files."""


import csv
import uuid
import argparse
import requests


AXON_WHITELIST = "users.tsv"
"""The filename of the default Axon/Synapse whitelist file."""
AXON_ADMINLIST = "admins.tsv"
"""The filename of the default Axon/Synapse admin list."""


__author__ = "boristats"
__copyright__ = "2020, Axon Contributors"
__license__ = "ISC License"


def parse(file: str) -> dict:
    """Parse the given configuration file into a dictionary it's contents."""
    data = dict()
    with open(file, "r") as f:
        reader = csv.reader(f, delimiter="\t")
        for name, id in reader:
            data[name] = id
    return data


def write(file: str, data: dict) -> None:
    """Write a configuration file from the given dictionary."""
    with open(file, "w+", newline='') as f:
        writer = csv.writer(f, delimiter="\t")
        writer.writerows(data.items())


def add_player(file: str, username: str, id: uuid.UUID) -> None:
    """Add the given username and UUID to the given file."""
    data = parse(file)
    if id in data:
        raise ValueError(f"{id} is already present in {file}")
    data[username] = id
    write(file, data)


def remove_player(file: str, username: str, id=None) -> None:
    """Remove the given username from the given file. If the player is already
    removed, do nothing. The 'id' argument has no purpose."""
    data = parse(file)
    if data.get(username, None):
        data.pop(username)
        write(file, data)


def uuid_from_name(username: str) -> uuid.UUID:
    """Request the UUID for a given minecraft username. Raises RuntimeError if 
    the request did not complete successfully."""
    req = requests.get(f"https://api.mojang.com/users/profiles/minecraft/{username}")
    if req.status_code == 200:
        data = req.json()
        uuid_hex = data["id"]
        return uuid.UUID(hex=uuid_hex)
    else:
        raise RuntimeError(f"Cant get UUID for {username}: {req.status_code}/{req.reason}")


def interactive():
    """Start the interactive manager"""
    print("Soon (tm)")


def _parse_arguments():
    parser = argparse.ArgumentParser(
        prog="manager.py",
        description="Management script for Axon/Synapse configuration files",
        epilog=f"Copyright (C) {__copyright__}. Licensed under the {__license__}"
    )
    parser.add_argument("-i", "--interactive", action="store_true",
        dest="interactive", help="run the interactive manager.")
    parser.add_argument("-u", "--uuids-file", default="uuids.tsv", dest="uuids_file",
        help="file to store uuid-username associations in", metavar="FILE")
    action_group = parser.add_mutually_exclusive_group(required=True)
    action_group.add_argument("-a", "--add", const=add_player, dest="action",
        action="store_const",
        help="run the add_player function against the target configuration files")
    action_group.add_argument("-r", "--remove", const=remove_player, dest="action",
        action="store_const",
        help="run the remove_player function against the target configuration files")
    config_group = parser.add_argument_group(title="Configuration targets")
    config_group.add_argument("-w", "--whitelist", const=AXON_WHITELIST, dest="targets",
        action="append_const", help="target the Axon/Synapse whitelist file (default for -a/-r)")
    config_group.add_argument("-m", "--admin", const=AXON_ADMINLIST, dest="targets",
        action="append_const", help="target the Axon/Synapse admin list (default for -r)")
    config_group.add_argument("-t", "--target", dest="targets",
        action="append", help="target the specified file", metavar="FILE")
    parser.add_argument("username", type=str,
        metavar="USERNAME", help="a username to be given to the action function")
    
    args = parser.parse_args()

    if args.targets is None:
        if args.action == add_player:
            args.targets = [AXON_WHITELIST]
        elif args.action == remove_player:
            args.targets = [AXON_WHITELIST, AXON_ADMINLIST]
    
    return args


def main():
    args = _parse_arguments()
    if args.interactive:
        interactive()
    
    targets = args.targets.copy()
    if args.action == add_player:
        targets.append(args.uuids_file)
    user_uuid = uuid_from_name(args.username)
    
    for target in targets:
        args.action(target, args.username, user_uuid)


if __name__ == "__main__":
    main()