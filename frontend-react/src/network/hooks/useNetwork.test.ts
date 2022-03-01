import { renderHook } from "@testing-library/react-hooks";

import { server } from "../../__mocks__/HistoryMockServer";
import { HistoryApi, Report } from "../api/History";

import { useNetwork } from "./useNetwork";

describe("useNetwork.ts", () => {
    /* Handles setup, refresh, and closing of mock service worker */
    beforeAll(() => server.listen());
    afterEach(() => server.resetHandlers());
    afterAll(() => server.close());

    test("positive response", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useNetwork<Report>(HistoryApi.detail("test"))
        );
        await waitForNextUpdate();

        expect(result.current.loading).toBeFalsy();
        expect(result.current.status).toBe(200);
        expect(result.current.message).toBe("");
        expect(result.current.data).toBeDefined();
    });

    test("negative response", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useNetwork<Report>(HistoryApi.detail("fail"))
        );
        await waitForNextUpdate();

        expect(result.current.loading).toBeFalsy();
        expect(result.current.status).toBe(404);
        expect(result.current.message).toBe(
            "Request failed with status code 404"
        );
        expect(result.current.data).toBeUndefined();
    });
});