import { useResource } from "rest-hooks";
import OrganizationResource from "../../resources/OrganizationResource";
import { useOktaAuth } from "@okta/okta-react";
import { groupToOrg } from "../../webreceiver-utils";
import HipaaNotice from "../../components/HipaaNotice";
import TableReports from "./Table/TableReports";
import { Suspense } from "react";
import Spinner from "../../components/Spinner";

const OrgName = () => {
    const { authState } = useOktaAuth();

    // finds the first organization that does not have the word "sender" in it
    const organization = groupToOrg(
        authState!.accessToken?.claims.organization.find(o => !o.toLowerCase().includes('sender'))
    );
    const org = useResource(OrganizationResource.detail(), {
        name: organization,
    });

    return (
        <span id="orgName" className="text-normal text-base">
            {org?.description}
        </span>
    );
};

function Daily() {

    return (
        <>
            <section className="grid-container margin-bottom-5">
                <Suspense fallback={<span className="text-normal text-base">Loading Info...</span>}>
                    <h3 className="margin-bottom-0">
                        <OrgName />
                    </h3>

                </Suspense>
                <h1 className="margin-top-0 margin-bottom-0">COVID-19</h1>
            </section>
            <Suspense fallback={<Spinner />}>
                <section className="grid-container margin-top-0"></section>
                <TableReports />
            </Suspense>
            <HipaaNotice />
        </>
    );
};

export default Daily