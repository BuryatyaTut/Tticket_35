import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.io.*;


public class LeitnerSystem {
    private List<Deque<String[]>> boxes;
    private final Random rand;
    private final ResourceBundle bundle;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 1) {
            System.out.println("Usage: java LeitnerSystem <input_file>");
            return;
        }

        LeitnerSystem system = new LeitnerSystem(Locale.getDefault());

        File stateFile = new File(args[0] + ".state");

        if(stateFile.exists()) {
            System.out.println(system.bundle.getString("continuePrompt"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String response = reader.readLine();

            if(response.equalsIgnoreCase(system.bundle.getString("yes"))) {
                system.loadStateFromFile(args[0] + ".state");
            } else {
                system.loadWordsFromFile(args[0]);
            }
        } else {
            system.loadWordsFromFile(args[0]);
        }

        system.printBoxes();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String[] wordPair = system.getNextWord();
                if (wordPair == null) {
                    break;
                }

                System.out.println(system.bundle.getString("translatePrompt") + " " + wordPair[0]);
                String translation = reader.readLine();
                if (translation.equals(":print")) {
                    system.printBoxes();
                    translation = reader.readLine();
                }

                if (translation.isEmpty()) {
                    system.saveStateToFile(args[0] + ".state");
                    break;
                } else if (translation.equals(wordPair[1])) {
                    system.correctAnswer(wordPair);
                } else {
                    system.wrongAnswer(wordPair);
                }
            }
        }
    }

    public LeitnerSystem(Locale locale) {
        this.boxes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            this.boxes.add(new ArrayDeque<>());
        }
        this.rand = new Random();
        this.bundle = ResourceBundle.getBundle("MessagesBundle", locale);
    }

    public void addWord(String[] wordPair, int box) {
        this.boxes.get(box).add(wordPair);
    }

    public String[] getNextWord() {
        int totalWeight = 0;
        for (int i = 0; i < 10; i++) {
            totalWeight += (int) (Math.pow(1.5, i) * this.boxes.get(i).size());
        }
        int randIndex = rand.nextInt(totalWeight);
        int currentWeight = 0;
        for (int i = 0; i < 10; i++) {
            currentWeight += (int) (Math.pow(1.5, i) * this.boxes.get(i).size());
            if (randIndex < currentWeight) {
                return this.boxes.get(i).peek();
            }
        }
        return null;
    }

    public void correctAnswer(String[] wordPair) {
        for (int i = 0; i < 10; i++) {
            if (this.boxes.get(i).contains(wordPair)) {
                this.boxes.get(i).remove(wordPair);
                if (i < 9) {
                    this.boxes.get(i + 1).add(wordPair);
                } else {
                    this.boxes.get(i).add(wordPair);
                }
                break;
            }
        }
    }

    public void wrongAnswer(String[] wordPair) {
        for (int i = 0; i < 10; i++) {
            if (this.boxes.get(i).contains(wordPair)) {
                this.boxes.get(i).remove(wordPair);
                this.boxes.get(0).add(wordPair);
                break;
            }
        }
    }

    public void printBoxes() {
        for (int i = 0; i < 10; i++) {
            System.out.println(bundle.getString("box") + " " + (i + 1) + ": ");
            for(String[] wordPair : this.boxes.get(i)) {
                System.out.println(wordPair[0]);
            }
        }
    }

    public void saveStateToFile(String filename) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(this.boxes);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadStateFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            this.boxes = (List<Deque<String[]>>) in.readObject();
        }
    }

    public void loadWordsFromFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    addWord(new String[]{parts[0].trim(), parts[1].trim()}, 0);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }
}

