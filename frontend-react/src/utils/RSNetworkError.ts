/** For consistency, when passing the code prop, please use these values
 * e.g. <ErrorComponent code={RSError.NOT_FOUND} /> */
export enum ErrorName {
    UNAUTHORIZED = "unauthorized", //401
    NOT_FOUND = "not-found", //404
    SERVER_ERROR = "server-error", //500-599
    // Any error thrown that cannot be parsed by RSError.parseStatus()
    UNKNOWN = "unknown-error",
    // Any generic error
    NON_RS_ERROR = "non-rs-error",
}

/** Throw from any failing network calls, and pass in the status code to
 * match it with the right display */
export class RSNetworkError extends Error {
    /* Used for identifying unique content to display for error */
    name: ErrorName;
    /* Build a new RSNetworkError */
    constructor(message: string, statusCode?: number) {
        super(message); // Sets message
        this.name = this.parseStatus(statusCode); // Sets code using child's parseStatus
        Object.setPrototypeOf(this, RSNetworkError.prototype);
    }
    /** Map response status code to error name */
    parseStatus(statusCode?: number) {
        switch (true) {
            case statusCode === 401:
                return ErrorName.UNAUTHORIZED;
            case statusCode === 404:
                return ErrorName.NOT_FOUND;
            case statusCode!! >= 500 && statusCode!! <= 599:
                return ErrorName.SERVER_ERROR;
            case statusCode === undefined:
            default:
                return ErrorName.UNKNOWN;
        }
    }
}
/** Easy boolean check to validate error is of RSError descent */
export const isRSNetworkError = (e: object) => e instanceof RSNetworkError;