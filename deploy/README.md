# Development deployment setup

The `dev` push workflow tests the app, pushes a commit-tagged image to ECR, and
uses SSM Run Command to deploy that exact image to the EC2 instance. Pull requests
only test and build; they do not deploy.

Complete these one-time steps before merging the CI/CD pull request:

1. Add the contents of `github-actions-ssm-policy.json` as an inline policy on
   `MulmiGitHubActionsRole`. This only permits Run Command on the development EC2
   instance and reads the command result. The existing ECR push policy stays.
2. Set the GitHub Actions repository variable `EC2_INSTANCE_ID` to
   `i-0c9e8d2a963a41f3b`. Keep `AWS_REGION=ap-northeast-2`,
   `ECR_REPOSITORY=mulmi-backend`, and `AWS_ROLE_ARN` as already configured.
3. In the EC2 Session Manager shell, run `sudo install -d -m 700 /opt/mulmi`.
   Edit `/opt/mulmi/backend.env` as root, using `backend.env.example` as a guide.
   Use the RDS endpoint from the RDS console and the password already chosen
   when creating the DB. Generate a separate JWT secret on EC2 with
   `openssl rand -hex 32`; do not paste it into GitHub or a chat. Run
   `sudo chmod 600 /opt/mulmi/backend.env` afterward. No AWS access keys are
   needed because EC2 has `MulmiEc2Role`.

The development Compose file temporarily uses Hibernate `ddl-auto=update`
because this database is empty and the project has no schema migrations yet.
Add migrations and switch back to `validate` before using this deployment
configuration for production.

After merging, the workflow should report `Deployment succeeded`. Check
`http://<EC2_PUBLIC_IP>/api/health` and `http://<EC2_PUBLIC_IP>/swagger`.
The EC2 public IP can change after a stop/start; use the current address in the
EC2 console. If deployment fails, inspect its SSM command ID in Systems Manager
Run Command and run `sudo docker logs mulmi-backend --tail 100` on EC2.
