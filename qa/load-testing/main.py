from argparse import ArgumentParser
import glob
import os
import requests
import sys
import time

DOCKER_HOST_IP=os.getenv('DOCKER_HOST_IP')
assert DOCKER_HOST_IP != None, "Failed to get DOCKER_HOST_IP from environment variable"
UPLOAD_URL = "http://%s:8080/upload/bsmlog" % DOCKER_HOST_IP

def get_list_of_files_in_directory(directory):
    files_and_directories = glob.glob(directory+"/**/*", recursive=True)
    files_only = []
    for filepath in files_and_directories:
        if os.path.isfile(filepath):
            files_only.append(filepath)
    return files_only

def upload_file(filepath):
    with open(filepath, 'rb') as file:
        return requests.post(UPLOAD_URL, files={'name':'file', 'file':file}, timeout=2)

def main():
    parser = ArgumentParser()
    deposit_method_group = parser.add_mutually_exclusive_group(required=True)
    deposit_method_group.add_argument("--deposit-rest", dest="deposit_rest", action='store_true', help="Deposit files to the ODE directly using the REST upload endpoint.")
    deposit_method_group.add_argument("--deposit-copy", dest="deposit_copy", action='store_true', help="Deposit files to the ODE indirectly by copying them into the uploads directory.")

    source_files_group = parser.add_mutually_exclusive_group(required=True)
    source_files_group.add_argument("--generate-files", dest="genfiles", action='store_true', help="If provided, the script will create duplicate temporary files from specified source file.")
    parser.add_argument("--source", dest="source", help="Required if --generate-files-from-source, specifies the reference file for temporary duplication.", required="--generate-files" in sys.argv)

    source_files_group.add_argument("--existing-files", dest="existingfiles", action='store_true', help="If provided, the script will deposit existing files in .")


    parser.add_argument("--dir", dest="dir", help="Directory containing files to upload.", required="--deposit-copy" in sys.argv)
    args = parser.parse_args()

    file_list = get_list_of_files_in_directory(args.dir)

    num_files = len(file_list)
    i = 1

    start_time = time.time()
    for filepath in file_list:
        upload_start_time = time.time()
        upload_response = upload_file(filepath)
        time_now = time.time()
        print("[%d/%d] Upload response received: %s %s. \t Upload time taken: %.3f \t Total time elapsed: %.3f" % (i, num_files, upload_response.status_code, upload_response.text, (time_now - upload_start_time), (time_now - start_time)))
        i += 1

if __name__ == "__main__":
    main()