import { rest } from "msw";
import { setupServer } from "msw/node";

import ReportResource from "../resources/ReportResource";

const handlers = [
    /* Successfully returns a Report */
    rest.get(
        "https://test.prime.cdc.gov/api/history/report/1",
        (req, res, ctx) => {
            return res(ctx.status(200), ctx.json(new ReportResource("1")));
        }
    ),
];

/* TEST SERVER TO USE IN `.test.ts` FILES */
export const historyServer = setupServer(...handlers);