@SuppressWarnings("all")
public class LetterChecker {
    public static void main(String[] args) {
        String s = "Communication";
        String checked = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int times = 0;
            if (checked.contains(c + "")) continue;
            for (int j = 0; j < s.length(); j++) {
                if (s.charAt(j) == c) {
                    times++;
                }
            }
            checked += c;
            System.out.println(c + " has appeared " + times + " times in \"" + s + "\"");
        }
    }
}
