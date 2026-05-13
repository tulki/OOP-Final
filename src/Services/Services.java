package Services;

public class Services {
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final MarkService markService;
    private final ResearchService researchService;
    private final ReportService reportService;
    private final NewsService newsService;
    private final Logger logger;

    public Services(UserRepository userRepository,
                    CourseService courseService,
                    MarkService markService,
                    ResearchService researchService,
                    ReportService reportService,
                    NewsService newsService,
                    Logger logger) {
        this.userRepository   = userRepository;
        this.courseService    = courseService;
        this.markService      = markService;
        this.researchService  = researchService;
        this.reportService    = reportService;
        this.newsService      = newsService;
        this.logger           = logger;
    }

    public UserRepository  getUserRepository()  { return userRepository; }
    public CourseService   getCourseService()   { return courseService; }
    public MarkService     getMarkService()     { return markService; }
    public ResearchService getResearchService() { return researchService; }
    public ReportService   getReportService()   { return reportService; }
    public NewsService     getNewsService()     { return newsService; }
    public Logger          getLogger()          { return logger; }

    public void saveAll() {
        userRepository.save();
        courseService.save();
        markService.save();
        researchService.save();
        newsService.save();
    }
}
