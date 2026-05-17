package Services;

public class Services {
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final MarkService markService;
    private final ResearchService researchService;
    private final ReportService reportService;
    private final NewsService newsService;
    private final MessageService messageService;
    private final RequestService requestService;
    private final Logger logger;

    public Services(UserRepository userRepository,
                    CourseService courseService,
                    MarkService markService,
                    ResearchService researchService,
                    ReportService reportService,
                    NewsService newsService,
                    MessageService messageService,
                    RequestService requestService,
                    Logger logger) {
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.markService = markService;
        this.researchService = researchService;
        this.reportService = reportService;
        this.newsService = newsService;
        this.messageService = messageService;
        this.requestService = requestService;
        this.logger = logger;
    }

    public UserRepository getUserRepository() { return userRepository; }
    public CourseService getCourseService() { return courseService; }
    public MarkService getMarkService() { return markService; }
    public ResearchService getResearchService() { return researchService; }
    public ReportService getReportService() { return reportService; }
    public NewsService getNewsService() { return newsService; }
    public MessageService getMessageService() { return messageService; }
    public RequestService getRequestService() { return requestService; }
    public Logger getLogger() { return logger; }

    public void saveAll() {
        userRepository.save();
        courseService.save();
        markService.save();
        researchService.save();
        newsService.save();
        messageService.save();
        requestService.save();
    }
}
