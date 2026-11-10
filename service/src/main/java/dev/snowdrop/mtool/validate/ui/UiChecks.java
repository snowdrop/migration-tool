package dev.snowdrop.mtool.validate.ui;

/**
 * A group of UI migration checks, e.g. the checks of one view technology.
 */
interface UiChecks {

    void run(UiContext context);
}
