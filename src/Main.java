import Services.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        UserRepository userRepository = UserRepository.load();
        MarkService markService = MarkService.load();
        CourseService courseService = CourseService.load(userRepository);
        ResearchService researchService = ResearchService.load(userRepository);
        ReportService reportService = new ReportService(markService, userRepository);
        NewsService newsService = NewsService.load();
        MessageService messageService = MessageService.load();
        RequestService requestService = RequestService.load();
        Logger logger = Logger.getInstance();

        Services services = new Services(
                userRepository, courseService, markService,
                researchService, reportService, newsService,
                messageService, requestService, logger);

        System.out.println("==========================================");
        System.out.println("  Research-Oriented University System");
        System.out.println("==========================================");
        
        Scanner scanner = new Scanner(System.in);
        while (true) {
            new Session(services, scanner).run();
            System.out.print("\nNew session? (y/n): ");
            if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) break;
        }
        scanner.close();
    }
}
