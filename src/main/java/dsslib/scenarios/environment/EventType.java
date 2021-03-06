package dsslib.scenarios.environment;

public enum EventType {
    FAIL_CLOUDLET,
    ENABLE_PROCESS,
    FAIL_PROCESS,
    FAIL_LINK,
    FAIL_LEADER,
    ENABLE_PREVIOUS_LEADER,
    ALL_CLOUDLETS_BELIEVE_THEY_ARE_LEADER,
    FAIL_CLOUDLETS_ONLY,
    FAIL_GUARDS,
    ENABLE_ALL_RANDOM,
    FAIL_IOTS_ONLY, FAIL_LINKS_IOT_TO_CLOUDLET
}
