import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

public class WordTestApp {
	private JFrame frame;
	private JTextArea resultArea;
	private JButton startButton;
	private JButton detailsButton;
	private JButton forwardButton;
	private JButton backButton;
	private JButton stopButton;
	private JTextField userInput;
	private JLabel progressLabel;
	private JLabel questionLabel;
	private JLabel statLabel;
	private JProgressBar progressBar;
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem chooseFileMenuItem, exitMenuItem;
	private JMenu settingsMenu;
	private JMenuItem setWordCountMenuItem, toggleCaseMenuItem, toggleTimerMenuItem;
	private JMenuItem toggleStatVisibilityMenuItem, setTimePerWordMenuItem; // Новий елемент для перемикання видимості
																			// статистики

	private boolean statVisible = true; // За замовчуванням статистика відображається
	private int testWordCount = 10;
	private List<String[]> wordPairs = new ArrayList<>();
	private List<String[]> originalWordPairs = new ArrayList<>();
	private List<String[]> wrongWords = new ArrayList<>();
	private int currentIndex = 0;
	private int correctCount = 0;
	private int wrongCount = 0;
	private boolean caseSensitive = false;
	private boolean testStarted = false;
	private boolean timerEnabled = true; // Секундомір включений за замовчуванням
	private Timer testTimer;
	private int timeRemaining; // Загальний час на всі слова (30 секунд на кожне слово)
	private int timePerWord = 30; // Час на кожне слово (30 секунд)

	private Map<Integer, String> answers = new HashMap<>(); // Збереження відповідей

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new WordTestApp().createAndShowGUI());
	}

	public void createAndShowGUI() {
		// Create the main frame
		frame = new JFrame("Перевірка слів");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 400); // Increased window size for better readability
		frame.setLocationRelativeTo(null);

		// Set modern look-and-feel for the UI
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Setup the menu bar with modern style
		menuBar = new JMenuBar();

		// File menu
		fileMenu = new JMenu("Меню");
		chooseFileMenuItem = new JMenuItem("Вибрати файл");
		exitMenuItem = new JMenuItem("Вихід");
		chooseFileMenuItem.addActionListener(e -> chooseFile());
		exitMenuItem.addActionListener(e -> System.exit(0));

		fileMenu.add(chooseFileMenuItem);
		fileMenu.add(exitMenuItem);
		menuBar.add(fileMenu);

		// Settings menu
		settingsMenu = new JMenu("Налаштування");
		setWordCountMenuItem = new JMenuItem("Кількість слів у тесті");
		toggleStatVisibilityMenuItem = new JCheckBoxMenuItem("Вкл/викл статистику");
		toggleCaseMenuItem = new JCheckBoxMenuItem("Вкл/викл регістр");
		toggleTimerMenuItem = new JCheckBoxMenuItem("Вкл/викл таймер");
		setTimePerWordMenuItem = new JMenuItem("Час для одного слова");

		// Спочатку встановлюємо статус кожного чекбоксу
		toggleStatVisibilityMenuItem.setSelected(statVisible);
		toggleCaseMenuItem.setSelected(caseSensitive);
		toggleTimerMenuItem.setSelected(timerEnabled);

		setWordCountMenuItem.addActionListener(e -> setWordCount());
		toggleCaseMenuItem.addActionListener(e -> toggleCaseSensitivity());
		toggleTimerMenuItem.addActionListener(e -> toggleTimer());
		toggleStatVisibilityMenuItem.addActionListener(e -> toggleStatVisibility());
		setTimePerWordMenuItem.addActionListener(e -> setTimePerWord());

		settingsMenu.add(setWordCountMenuItem);
		settingsMenu.add(toggleCaseMenuItem);
		settingsMenu.add(toggleTimerMenuItem);
		settingsMenu.add(toggleStatVisibilityMenuItem);
		settingsMenu.add(setTimePerWordMenuItem);

		settingsMenu.add(Box.createHorizontalGlue()); // Push settings menu to the right
		menuBar.add(settingsMenu);
		frame.setJMenuBar(menuBar);

		// Layout setup
		frame.setLayout(new BorderLayout());

		// Center panel (contains the question, input field, and progress)
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		questionLabel = new JLabel("Натисніть кнопку старт...");
		questionLabel.setFont(new Font("Roboto", Font.PLAIN, 18)); // Use modern font
		questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center label
		centerPanel.add(questionLabel);

		userInput = new JTextField(20);
		userInput.setFont(new Font("Roboto", Font.PLAIN, 16)); // Modern font for input
		userInput.addActionListener(e -> checkAnswer());
		centerPanel.add(userInput);

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		centerPanel.add(progressBar);

		progressLabel = new JLabel("Часу залишилось: 00:00");
		progressLabel.setFont(new Font("Roboto", Font.PLAIN, 14));

		// Центрована панель для таймера
		JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		timerPanel.add(progressLabel);
		centerPanel.add(timerPanel);

		frame.add(centerPanel, BorderLayout.CENTER);

		// Bottom panel (contains stats and control buttons)
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new GridLayout(3, 1));

		statLabel = new JLabel("Вірно: 0 | Помилки: 0");
		statLabel.setFont(new Font("Roboto", Font.PLAIN, 14)); // Modern font for stats
		statLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center label
		bottomPanel.add(statLabel);

		detailsButton = new JButton("Деталі");
		detailsButton.addActionListener(e -> showDetails());
		detailsButton.setEnabled(false); // Enable after test completion
		bottomPanel.add(detailsButton);

		frame.add(bottomPanel, BorderLayout.SOUTH);

		// Control buttons (Start, Back, Forward, Stop)
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10)); // Center and space out buttons

		// Start button
		startButton = new JButton("Старт");
		startButton.setBackground(new Color(34, 193, 195)); // Soft turquoise green
		startButton.setFont(new Font("Roboto", Font.PLAIN, 16)); // Modern font
		startButton.setForeground(Color.BLACK); // Black text on green
		startButton.setFocusPainted(false); // Remove focus border for better look
		startButton.addActionListener(e -> startTest());
		controlPanel.add(startButton);

		// Back button
		backButton = new JButton("Назад");
		backButton.setBackground(new Color(70, 130, 180)); // Steel blue
		backButton.setFont(new Font("Roboto", Font.PLAIN, 16));
		backButton.setForeground(Color.BLACK);
		backButton.setFocusPainted(false);
		backButton.setEnabled(false);
		backButton.addActionListener(e -> moveBack());
		controlPanel.add(backButton);

		// Forward button
		forwardButton = new JButton("Вперед");
		forwardButton.setBackground(new Color(70, 130, 180)); // Steel blue
		forwardButton.setFont(new Font("Roboto", Font.PLAIN, 16));
		forwardButton.setForeground(Color.BLACK);
		forwardButton.setFocusPainted(false);
		forwardButton.setEnabled(false);
		forwardButton.addActionListener(e -> moveForward());
		controlPanel.add(forwardButton);

		// Stop button
		stopButton = new JButton("Стоп");
		stopButton.setBackground(Color.RED); // Red color for Stop
		stopButton.setFont(new Font("Roboto", Font.PLAIN, 16));
		stopButton.setForeground(Color.BLACK); // White text on red
		stopButton.setFocusPainted(false);
		stopButton.setEnabled(false);
		stopButton.addActionListener(e -> stopTest());
		controlPanel.add(stopButton);

		frame.add(controlPanel, BorderLayout.NORTH);

		// Text area for result and errors
		resultArea = new JTextArea();
		resultArea.setEditable(false);
		resultArea.setFont(new Font("Roboto", Font.PLAIN, 14));
		resultArea.setVisible(false); // Initially hidden
		frame.add(new JScrollPane(resultArea), BorderLayout.EAST);

		frame.setVisible(true);
		userInput.setEditable(false);

		loadDefaultFile();

	}

	private void loadDefaultFile() {
		Path filePath = Paths.get(System.getProperty("user.home"), "Desktop", "words.txt");
		if (Files.exists(filePath)) {
			loadWordsFromFile(filePath);
		}
	}
	
	private void chooseFile() {
		// Скидання стану тесту перед вибором нового файлу
		if (testStarted) {
			stopTest(); // зупиняємо поточний тест
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files", "txt"));
		int result = fileChooser.showOpenDialog(frame);
		if (result == JFileChooser.APPROVE_OPTION) {
			loadWordsFromFile(fileChooser.getSelectedFile().toPath());
		}
	}
	
	private void loadWordsFromFile(Path filePath) {
		wordPairs.clear();
		originalWordPairs.clear();
		try (BufferedReader br = Files.newBufferedReader(filePath)) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(" - ");
				if (parts.length == 2) {
					wordPairs.add(parts);
					originalWordPairs.add(parts); // Keep a copy of the original list
				}
			}

			if (!wordPairs.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Слова успішно завантажено. Кількість слів: " + wordPairs.size());
			} else {
				JOptionPane.showMessageDialog(frame, "Невірний формат слів.");
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame, "Помилка завантаження файлу.");
		}
	}
	
	private void startTest() {
		//testCount++;

		if (wordPairs.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Немає доступних слів для початку тесту.");
			return;
		}
		
		testStarted = true;
		correctCount = 0;
		wrongCount = 0;
		currentIndex = 0;
		wrongWords.clear(); 
		answers.clear();
		progressBar.setValue(0);
		statLabel.setText("Вірно: 0 | Пимилки: 0");
		detailsButton.setEnabled(false);
		startButton.setEnabled(false);
		stopButton.setEnabled(true);
		userInput.setEditable(true);
		
		if (!originalWordPairs.isEmpty()) {
			wordPairs.clear();
			Collections.shuffle(originalWordPairs);
			wordPairs.addAll(originalWordPairs.subList(0, Math.min(testWordCount, originalWordPairs.size())));
		}

		timeRemaining = testWordCount * timePerWord; // Set total time for the test

		// Start the overall timer
		if (timerEnabled) {
			testTimer = new Timer(1000, e -> updateTimer());
			testTimer.start();
		} else {
			progressLabel.setVisible(false); // Сховати таймер, якщо він вимкнений
		}
		
		showNextQuestion();

	}
	
	private void stopTest() {
		if (testStarted) {
			// Зупинити таймер, якщо він працює
			if (testTimer != null && testTimer.isRunning()) {
				testTimer.stop();
			}

			// За кожне невведене слово зараховуємо помилку
			for (int i = currentIndex; i < wordPairs.size(); i++) {
				if (!answers.containsKey(i)) { // Якщо відповідь ще не введена
					wrongCount++; // Збільшуємо кількість помилок
					wrongWords.add(wordPairs.get(i)); // Додаємо слово до списку помилок
				}
			}
			
			// Оновлюємо статистику
			statLabel.setText("Вірно: " + correctCount + " | Помилки: " + wrongCount);

			// Показуємо кінцеву статистику
			showFinalStatistics();
			
			// Відключаємо кнопки після зупинки
			backButton.setEnabled(false);
			forwardButton.setEnabled(false);
			stopButton.setEnabled(false);
			startButton.setEnabled(true); // Кнопка Старт знову доступна для перезапуску
			userInput.setEditable(false);

			currentIndex = 0;
		}
	}
	
	private void moveBack() {
		if (currentIndex > 0) {
			currentIndex--;
			showNextQuestion();
		}
	}

	private void moveForward() {
		if (currentIndex < wordPairs.size() - 1) {
			checkAnswer();
		}
	}
	
	private void setTimePerWord() {
		String input = JOptionPane.showInputDialog(frame, "Введіть час для кожного слова в секундах:");
		try {
			int time = Integer.parseInt(input);
			if (time > 0 && time <= 600) { // Можна обмежити максимальний час (наприклад, 600 секунд = 10 хвилин)
				timePerWord = time;
				JOptionPane.showMessageDialog(frame, "Час для кожного слова " + timePerWord + " секунд.");
			} else {
				JOptionPane.showMessageDialog(frame, "Будь ласка, введіть число від 1 до 600.");
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(frame, "Помилка. Введіть коректне число.");
		}
	}

	private void setWordCount() {
		String input = JOptionPane.showInputDialog(frame, "Введіть кількість слів у тесті:");
		try {
			int count = Integer.parseInt(input);
			if (count > 0 && count <= originalWordPairs.size()) { // Перевірка на повний список
				testWordCount = count;
				JOptionPane.showMessageDialog(frame, "Тест складатиметься з " + testWordCount + " слів.");
			} else {
				JOptionPane.showMessageDialog(frame,
						"Некоректне число. Введіть число від 1 до " + originalWordPairs.size());
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(frame, "Помилка. Введіть коректне число.");
		}
	}
	
	private void toggleStatVisibility() {
		statLabel.setVisible(!statLabel.isVisible());
	}

	private void toggleCaseSensitivity() {
		caseSensitive = !caseSensitive;
		String message = caseSensitive ? "Регістр включено." : "Регістр виключено.";
		JOptionPane.showMessageDialog(frame, message);
	}

	private void toggleTimer() {
		timerEnabled = !timerEnabled;
		String message = timerEnabled ? "Таймер включений." : "Таймер виключений.";
		JOptionPane.showMessageDialog(frame, message);

		// Перемикаємо видимість таймера
		progressLabel.setVisible(timerEnabled); // Таймер буде ховатися, якщо вимкнений
	}

	private void updateTimer() {
		if (timeRemaining > 0) {
			timeRemaining--;
			// Оновлюємо тільки таймер, без прогресу
			progressLabel.setText("Часу залишилось: " + formatTime(timeRemaining));
		} else {
			checkAnswer(); // Автоматично перевіряємо відповідь, якщо час вичерпано
		}
	}

	private void showNextQuestion() {
		if (currentIndex < wordPairs.size()) {
			String[] currentWordPair = wordPairs.get(currentIndex);
			questionLabel.setText("Перекладіть: " + currentWordPair[1]);

			updateProgressBar();
			// Check if there's already an answer for the current word
			String savedAnswer = answers.get(currentIndex);
			if (savedAnswer != null) {
				userInput.setText(savedAnswer); // Display saved answer
			} else {
				userInput.setText(""); // Clear input if no answer is saved
			}

			// Clear the input field before each new word if no saved answer
			userInput.requestFocus();

			// Enable or disable the forward and back buttons depending on the state
			backButton.setEnabled(currentIndex > 0);
			forwardButton.setEnabled(currentIndex < wordPairs.size() - 1);
		} else {
			showFinalStatistics();
		}
	}

	private void updateProgressBar() {
		int progress = (int) (((double) (currentIndex + 1) / wordPairs.size()) * 100);
		progressBar.setValue(progress);
	}

	private void checkAnswer() {
		if (currentIndex >= wordPairs.size()) {
			return;
		}

		String userAnswer = userInput.getText().trim();
		String correctAnswer = wordPairs.get(currentIndex)[0];

		// Якщо регістр не враховується, приводимо обидва слова до одного формату
		// (наприклад, до маленьких літер)
		if (!caseSensitive) {
			correctAnswer = correctAnswer.toLowerCase();
			userAnswer = userAnswer.toLowerCase();
		}

		// Перевірка відповіді
		boolean isCorrect = userAnswer.equals(correctAnswer);

		// Якщо відповідь була неправильна раніше і тепер стала правильною
		if (answers.containsKey(currentIndex)) {
			String previousAnswer = answers.get(currentIndex);

			// Якщо попередня відповідь була неправильною і зараз вона правильна
			if (!previousAnswer.equals(correctAnswer) && isCorrect) {
				wrongCount--; // Зменшуємо кількість неправильних відповідей
				correctCount++; // Збільшуємо кількість правильних відповідей
				wrongWords.removeIf(pair -> pair[1].equals(wordPairs.get(currentIndex)[1])); // Видаляємо це слово з
																								// wrongWords
			}
			// Якщо попередня відповідь була правильною, а тепер стала неправильною
			else if (previousAnswer.equals(correctAnswer) && !isCorrect) {
				correctCount--; // Зменшуємо кількість правильних відповідей
				wrongCount++; // Збільшуємо кількість неправильних відповідей
				wrongWords.add(wordPairs.get(currentIndex)); // Додаємо це слово в wrongWords
			}
		}

		// Якщо відповідь правильна
		if (isCorrect) {
			if (!answers.containsKey(currentIndex)) {
				correctCount++;
			}
		} else {
			if (!answers.containsKey(currentIndex)) {
				wrongCount++;
				wrongWords.add(wordPairs.get(currentIndex));
			}
		}

		// Зберігаємо відповідь користувача
		answers.put(currentIndex, userAnswer);

		// Оновлюємо статистику
		statLabel.setText("Вірно: " + correctCount + " | Помилки: " + wrongCount);

		// Переходимо до наступного слова
		currentIndex++;

		if (currentIndex == wordPairs.size()) {
			// Зупиняємо таймер після останнього слова
			if (testTimer != null && testTimer.isRunning()) {
				testTimer.stop();
			}
		}

		showNextQuestion(); // Перехід до наступного питання
	}

	private void showDetails() {
		JDialog detailsDialog = new JDialog(frame, "Деталі", true);
		detailsDialog.setSize(550, 200);
		detailsDialog.setLocationRelativeTo(frame);

		JTextArea detailsArea = new JTextArea();
		detailsArea.setEditable(false);
		detailsArea.setFont(new Font("Arial", Font.PLAIN, 14));

		StringBuilder wrongWordsList = new StringBuilder();
		if (wrongWords.isEmpty()) {
			wrongWordsList.append("Немає помилок.");
		} else {
			for (String[] wordPair : wrongWords) {
				// Використовуємо правильний індекс для того, щоб отримати відповідь користувача
				int index = wordPairs.indexOf(wordPair); // Знаходимо індекс слова в основному списку
				String userAnswer = answers.get(index); // Отримуємо відповідь користувача для цього слова

				if (userAnswer == null || userAnswer.isEmpty()) {
					userAnswer = "Немає відповіді"; // Якщо немає відповіді, заміняємо на "null" або інший індикатор
				}

				wrongWordsList.append(wordPair[1]).append(" -> ").append(wordPair[0]).append(" (Ваш варіант: ")
						.append(userAnswer).append(")\n");
			}
		}

		detailsArea.setText(wrongWordsList.toString());
		JScrollPane scrollPane = new JScrollPane(detailsArea);
		detailsDialog.add(scrollPane);

		detailsDialog.setVisible(true);
	}

	private void showFinalStatistics() {
		testStarted = false;
		userInput.setEditable(false);
		double correctPercentage = (double) correctCount / wordPairs.size() * 100;
		String grade = getGrade(correctPercentage);

		String stats = String.format("Тест завершено!\nВірно: %.2f%%\nОцінка: %s", correctPercentage, grade);

		// Показуємо статистику лише якщо вона видима
		if (statVisible) {
			JOptionPane.showMessageDialog(frame, stats);
		}

		// Enable the "Details" button for mistakes
		detailsButton.setEnabled(true);
		startButton.setEnabled(true);

		// Disable forward and back buttons after the test is complete
		backButton.setEnabled(false);
		forwardButton.setEnabled(false);
		stopButton.setEnabled(false);
	}

	private String getGrade(double percentage) {
		if (percentage >= 95)
			return "1";
		else if (percentage >= 85)
			return "2";
		else if (percentage >= 75)
			return "3";
		else if (percentage >= 65)
			return "4";
		else if (percentage >= 50)
			return "5";
		else
			return "6";
	}

	private String formatTime(int timeInSeconds) {
		int minutes = timeInSeconds / 60;
		int seconds = timeInSeconds % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}
}