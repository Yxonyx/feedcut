# Security

Feedcut has no network code and no `INTERNET` permission, so most of the usual attack surface does not exist. What matters is the accessibility service: it can see the apps you switch on and press Back, Home or a tab.

## Reporting

Please report a vulnerability privately through GitHub: open the repository's **Security** tab and choose **Report a vulnerability**. Do not open a public issue for it.

You can expect a first answer within a week. Useful reports say what an attacker could do, on which Android version, and how to reproduce it.

## In scope

- The service reading more than view ids and the Facebook tab bar labels.
- Another app making Feedcut act (press Back/Home, change rules) without the user.
- Rules or counters leaving the phone in any way.
- The release signing or the build pipeline.

## Supported versions

Only the latest release gets fixes.
