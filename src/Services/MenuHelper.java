package Services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public final class MenuHelper {
    private MenuHelper() {}

    public static <T> T select(Scanner in, String prompt, T[] options) {
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  %d - %s%n", i + 1, options[i]);
        }
        System.out.print(prompt);
        try {
            int index = Integer.parseInt(in.nextLine().trim()) - 1;
            if (index < 0 || index >= options.length) {
                throw new IllegalArgumentException("Invalid selection.");
            }
            return options[index];
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid selection.");
        }
    }

    public static <T> Set<T> selectMultiple(Scanner in, String prompt, T[] options) {
        Set<T> selected = new LinkedHashSet<>();
        while (true) {
            List<T> remaining = new ArrayList<>();
            for (T option : options) {
                if (!selected.contains(option)) remaining.add(option);
            }

            if (!selected.isEmpty()) {
                System.out.println("Selected: " + selected);
                if (remaining.isEmpty()) {
                    System.out.println("All options selected.");
                    break;
                }
                System.out.print("Add another? (y/n): ");
                if (!"y".equalsIgnoreCase(in.nextLine().trim())) break;
            }

            for (int i = 0; i < remaining.size(); i++) {
                System.out.printf("  %d - %s%n", i + 1, remaining.get(i));
            }
            System.out.print(prompt);
            try {
                int index = Integer.parseInt(in.nextLine().trim()) - 1;
                if (index < 0 || index >= remaining.size()) {
                    throw new IllegalArgumentException("Invalid selection.");
                }
                selected.add(remaining.get(index));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid selection.");
            }
        }
        if (selected.isEmpty()) throw new IllegalArgumentException("At least one selection is required.");
        return selected;
    }
}
