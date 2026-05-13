import Services.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        UserRepository userRepository = UserRepository.load();
        MarkService markService         = MarkService.load();
        CourseService courseService     = CourseService.load(userRepository);
        ResearchService researchService = ResearchService.load(userRepository);
        ReportService reportService     = new ReportService(markService, userRepository);
        NewsService newsService         = NewsService.load();
        Logger logger                   = Logger.getInstance();

        Services services = new Services(
                userRepository, courseService, markService,
                researchService, reportService, newsService, logger);

        Scanner scanner = new Scanner(System.in);
        new Session(services, scanner).run();
        scanner.close();
    }
}
