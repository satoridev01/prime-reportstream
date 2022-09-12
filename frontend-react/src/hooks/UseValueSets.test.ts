import { rest } from "msw";
import { renderHook, act } from "@testing-library/react-hooks";
import { QueryClient } from "@tanstack/react-query";

import { lookupTableServer } from "../__mocks__/LookupTableMockServer";
import {
    LookupTables,
    lookupTablesEndpoints,
    ValueSet,
} from "../config/endpoints/lookupTables";
import { QueryWrapper } from "../utils/CustomRenderUtils";

import {
    useValueSetsMeta,
    useValueSetsTable,
    useValueSetActivation,
    useValueSetUpdate,
} from "./UseValueSets";

describe("useValueSetsTable", () => {
    const renderWithQueryWrapper = (tableName: LookupTables) =>
        renderHook(() => useValueSetsTable<ValueSet[]>(tableName), {
            wrapper: QueryWrapper(
                new QueryClient({
                    // to allow for faster testable failures
                    defaultOptions: { queries: { retry: false } },
                })
            ),
        });

    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    test("returns expected data values when fetching table version", async () => {
        const { result, waitFor } = renderWithQueryWrapper(
            LookupTables.VALUE_SET
        );

        await waitFor(() => !!result.current.valueSetArray.length);
        const { name, system } = result.current.valueSetArray[0];
        expect(name).toEqual("sender_automation_value_set");
        expect(system).toEqual("LOCAL");
    });

    test("returns error when table data call errors", async () => {
        lookupTableServer.use(
            rest.get(
                lookupTablesEndpoints.getTableData.toDynamicUrl({
                    version: "2",
                    tableName: "sender_automation_value_set",
                }),
                (_req, res, ctx) => {
                    return res.once(ctx.json([]), ctx.status(400));
                }
            )
        );
        const { result, waitFor } = renderWithQueryWrapper(
            LookupTables.VALUE_SET
        );
        await waitFor(() => result.current.error);
        expect(result.current.error.message).toEqual(
            "Request failed with status code 400"
        );
    });
});

describe("useValueSetsMeta", () => {
    const renderWithQueryWrapper = (tableName?: LookupTables) =>
        renderHook(() => useValueSetsMeta(tableName), {
            wrapper: QueryWrapper(
                new QueryClient({
                    // to allow for faster testable failures
                    defaultOptions: { queries: { retry: false } },
                })
            ),
        });

    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    test("returns expected meta values", async () => {
        const { result, waitFor } = renderWithQueryWrapper();

        await waitFor(() => !!result.current.valueSetMeta.createdAt);
        const { createdAt, createdBy } = result.current.valueSetMeta;
        expect(createdAt).toEqual("now");
        expect(createdBy).toEqual("test@example.com");
    });

    test("returns expected meta values when passed an optional table name", async () => {
        const { result, waitFor } = renderWithQueryWrapper(
            LookupTables.VALUE_SET_ROW
        );

        await waitFor(() => !!result.current.valueSetMeta.createdAt);
        const { createdAt, createdBy } = result.current.valueSetMeta;
        expect(createdAt).toEqual("later");
        expect(createdBy).toEqual("again@example.com");
    });

    test("returns empty metadata when the passed table name doesn't exist in returned list of tables", async () => {
        const { result, waitFor } = renderWithQueryWrapper();
        await waitFor(() => !!result.current.valueSetMeta);
        expect(result.current.valueSetMeta).toEqual({});
    });

    test("returns error when table list call errors", async () => {
        lookupTableServer.use(
            rest.get(
                lookupTablesEndpoints.getTableList.url,
                (_req, res, ctx) => {
                    return res.once(ctx.json([]), ctx.status(400));
                }
            )
        );
        const { result, waitForNextUpdate } = renderWithQueryWrapper(
            LookupTables.VALUE_SET
        );
        await waitForNextUpdate();
        expect(result.current.error.message).toEqual(
            "Request failed with status code 400"
        );
    });
});

// note that running the mutation tests below results in a warning:
// `Can't perform a React state update on an unmounted component`
// I am unable to find the root of the problem here though I imagine it has
// to do with the query provider somehow not cleaning up after itself when the tests
// complete. As this comment shows, this error is being phased out in future
// React versions. This is annoying but I don't plan to spend any more time on it. -DWS
// https://github.com/reactwg/react-18/discussions/82
describe("useValueSetUpdate", () => {
    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    const renderWithQueryWrapper = () =>
        renderHook(() => useValueSetUpdate(), {
            wrapper: QueryWrapper(),
        });

    test("returns trigger, loading indicator and error", async () => {
        const { result } = renderWithQueryWrapper();
        const { saveData, saveError, isSaving } = result.current;
        expect(saveError).toEqual(null);
        expect(isSaving).toEqual(false);
        expect(saveData).toBeInstanceOf(Function);
    });

    test("mutation trigger returns expected values and tracks loading state", async () => {
        const { result, waitForNextUpdate } = renderWithQueryWrapper();
        const { saveData, isSaving } = result.current;
        expect(isSaving).toEqual(false);

        let saveResult;
        await act(async () => {
            const savePromise = saveData({
                data: [
                    {
                        name: "a-path",
                        display: "hi over here first value",
                        code: "1",
                        version: "1",
                    },
                    {
                        name: "a-path",
                        display: "test, yes, second value",
                        code: "2",
                        version: "1",
                    },
                ],
                tableName: "any",
            });
            await waitForNextUpdate();
            expect(result.current.isSaving).toEqual(true);
            saveResult = await savePromise;
        });
        expect(saveResult).toEqual({
            lookupTableVersionId: 2,
            tableName: "sender_automation_value_set_row",
            tableVersion: 2,
            isActive: true,
            createdBy: "again@example.com",
            createdAt: "later",
            tableSha256Checksum: "checksum",
        });
    });
});

describe("useValueSetActivation", () => {
    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    const renderWithQueryWrapper = () =>
        renderHook(() => useValueSetActivation(), {
            wrapper: QueryWrapper(),
        });

    test("returns trigger, loading indicator and error", async () => {
        const { result } = renderWithQueryWrapper();
        const { activateTable, activationError, isActivating } = result.current;
        expect(activationError).toEqual(null);
        expect(isActivating).toEqual(false);
        expect(activateTable).toBeInstanceOf(Function);
    });

    test("mutation trigger returns expected values and tracks loading state", async () => {
        const { result, waitForNextUpdate } = renderWithQueryWrapper();
        const { activateTable, isActivating } = result.current;
        expect(isActivating).toEqual(false);

        let activateResult;
        await act(async () => {
            const activationPromise = activateTable({
                tableVersion: 1,
                tableName: "any",
            });
            await waitForNextUpdate();
            expect(result.current.isActivating).toEqual(true);
            activateResult = await activationPromise;
        });

        expect(activateResult).toEqual({
            lookupTableVersionId: 2,
            tableName: "sender_automation_value_set_row",
            tableVersion: 2,
            isActive: true,
            createdBy: "again@example.com",
            createdAt: "later",
            tableSha256Checksum: "checksum",
        });
    });
});