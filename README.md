public String[] findAllFilePaths(Session session, String filename) {
    List<String> results = new ArrayList<>();

    try {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        String command = "find / -type f -name \"" + filename + "\" 2>/dev/null";
        channel.setCommand(command);

        InputStream input = channel.getInputStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                results.add(line);   // add each found path
            }
        }

        channel.disconnect();

    } catch (Exception e) {
        e.printStackTrace();
    }

    return results.toArray(new String[0]);
}
