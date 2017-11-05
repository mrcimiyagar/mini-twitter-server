package models.packets.base;

public enum AnswerStatus {
    OK,

    ERROR_100, // Request type was not detected
    ERROR_101, // Authentication data was incorrect

    ERROR_200, // Target human of follow request does not exist
    ERROR_201, // Target human is already being followed by you
    ERROR_202, // Your human data does not exists, login again
    ERROR_203, // Your connection is not authenticated yet
    ERROR_204, // Target human is already being requested by you

    ERROR_300, // Target human is not being followed by you
    ERROR_301, // Your human data does not exists, login again
    ERROR_302, // Your connection is not authenticated yet

    ERROR_400, // There was error in sql database
    ERROR_401, // Creating tweet content was empty
    ERROR_402, // Your human data does not exists, login again
    ERROR_403, // Your connection is not authenticated yet
    ERROR_404, // Requested parent tweet not found
    ERROR_405, // Target human page id not found
    ERROR_406, // Access to page denied

    ERROR_500, // Target human not found
    ERROR_501, // Your connection is not authenticated yet
    ERROR_502, // Access to target page denied

    ERROR_600, // Your connection is not authenticated yet

    ERROR_700, // Target Human id not found
    ERROR_701, // Your connection is not authenticated yet

    ERROR_800, // Target Human id not found
    ERROR_801, // Your connection is not authenticated yet
    ERROR_802, // Access to target page denied

    ERROR_900, // Target Human id not found
    ERROR_901, // Your connection is not authenticated yet
    ERROR_902, // Access to target page denied

    ERROR_1000, // User bio can't be empty
    ERROR_1001, // Your connection is not authenticated yet

    ERROR_1100, // Target tweet has been already like by you
    ERROR_1101, // Target tweet does not exist
    ERROR_1102, // Target page does not exist
    ERROR_1103, // Your connection is not authenticated yet

    ERROR_1200, // Target tweet has not been liked by you yet
    ERROR_1201, // Target tweet does not exist
    ERROR_1202, // Target page does not exist
    ERROR_1203, // Your connection is not authenticated yet

    ERROR_1300, // Your connection is not authenticated yet

    ERROR_1400, // Your connection is not authenticated yet

    ERROR_1500, // Your connection is not authenticated yet

    ERROR_1600, // Your connection is not authenticated yet

    ERROR_1700, // Your connection is not authenticated yet

    ERROR_1800, // Your connection is not authenticated yet

    ERROR_1900, // Your connection is not authenticated yet
    ERROR_1901, // Target tweet by id does not exist
    ERROR_1902, // Access to target tweet denied
}