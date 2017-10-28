package models.packets.base;

public enum AnswerStatus {
    OK,

    ERROR_100, // Request type was not detected
    ERROR_101, // Authentication data was incorrect

    ERROR_200, // Target human of follow request does not exist
    ERROR_201, // Target human is already being followed by you
    ERROR_202, // Your human data does not exists, login again
    ERROR_203, // Your connection is not authenticated yet

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

    ERROR_600, // Your connection is not authenticated yet

    ERROR_700, // Target Human id not found
    ERROR_701, // Your connection is not authenticated yet

    ERROR_800, // Target Human id not found
    ERROR_801, // Your connection is not authenticated yet
}