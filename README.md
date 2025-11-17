String cmd = "cd \"$(dirname \\\"$(find / -type f -name \\\"config.yaml\\\" -print -quit 2>/dev/null)\\\")\"";
