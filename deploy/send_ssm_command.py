"""Deploy the image built for this commit to the development EC2 instance."""

import base64
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time


def required(name: str, pattern: str) -> str:
    value = os.environ.get(name, "")
    if not re.fullmatch(pattern, value):
        raise SystemExit(f"Missing or invalid {name}")
    return value


def aws(*args: str) -> dict:
    try:
        result = subprocess.run(
            ["aws", *args, "--output", "json"],
            check=True,
            text=True,
            capture_output=True,
        )
    except subprocess.CalledProcessError as error:
        raise RuntimeError(error.stderr.strip()) from error
    return json.loads(result.stdout)


def encoded(path: str) -> str:
    return base64.b64encode(Path(path).read_bytes()).decode("ascii")


region = required("AWS_REGION", r"[a-z]{2}-[a-z]+-\d")
instance_id = required("EC2_INSTANCE_ID", r"i-[0-9a-f]{8,17}")
repository = required("ECR_REPOSITORY", r"[a-z0-9]+(?:[._/-][a-z0-9]+)*")
image_tag = required("IMAGE_TAG", r"[0-9a-f]{40}")
registry = required(
    "ECR_REGISTRY", r"\d{12}\.dkr\.ecr\.[a-z]{2}-[a-z]+-\d\.amazonaws\.com"
)
account_id, _, registry_region = registry.partition(".dkr.ecr.")
if registry_region != f"{region}.amazonaws.com":
    raise SystemExit("ECR_REGISTRY and AWS_REGION do not match")

commands = [
    "set -eu",
    "install -d -m 700 /opt/mulmi",
    f"printf %s {encoded('compose.deploy.yml')} | base64 -d > /opt/mulmi/compose.deploy.yml",
    f"printf %s {encoded('deploy/deploy-on-ec2.sh')} | base64 -d > /opt/mulmi/deploy-on-ec2.sh",
    "chmod 600 /opt/mulmi/compose.deploy.yml",
    "chmod 700 /opt/mulmi/deploy-on-ec2.sh",
    (
        f"AWS_REGION={region} AWS_ACCOUNT_ID={account_id} "
        f"ECR_REPOSITORY={repository} IMAGE_TAG={image_tag} "
        "bash /opt/mulmi/deploy-on-ec2.sh"
    ),
]

sent = aws(
    "ssm",
    "send-command",
    "--region",
    region,
    "--instance-ids",
    instance_id,
    "--document-name",
    "AWS-RunShellScript",
    "--comment",
    f"Deploy mulmi-backend {image_tag[:12]}",
    "--parameters",
    json.dumps({"commands": commands, "executionTimeout": ["600"]}),
)
command_id = sent["Command"]["CommandId"]
print(f"SSM command: {command_id}", flush=True)

deadline = time.monotonic() + 630
while time.monotonic() < deadline:
    time.sleep(5)
    try:
        invocation = aws(
            "ssm",
            "get-command-invocation",
            "--region",
            region,
            "--command-id",
            command_id,
            "--instance-id",
            instance_id,
        )
    except RuntimeError as error:
        # Run Command can take a moment to create the invocation record.
        if "InvocationDoesNotExist" in str(error):
            continue
        raise

    status = invocation["Status"]
    if status in {"Pending", "InProgress", "Delayed"}:
        continue
    print(invocation.get("StandardOutputContent", ""), end="")
    print(invocation.get("StandardErrorContent", ""), end="", file=sys.stderr)
    if status != "Success":
        raise SystemExit(f"Deployment failed: {status}")
    print("Deployment succeeded")
    break
else:
    raise SystemExit(f"Deployment timed out; inspect SSM command {command_id}")
