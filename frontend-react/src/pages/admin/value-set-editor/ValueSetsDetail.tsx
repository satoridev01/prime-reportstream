import React from "react";
import { Helmet } from "react-helmet";
import { useParams } from "react-router-dom";

import Table, {
    ColumnConfig,
    DatasetAction,
    TableConfig,
} from "../../../components/Table/Table";
import { useValueSetsRowTable } from "../../../hooks/UseLookupTable";
import { toHumanReadable } from "../../../utils/misc";

const valueSetDetailColumnConfig: ColumnConfig[] = [
    {
        dataAttr: "display",
        columnHeader: "Display",
        editable: true,
    },
    {
        dataAttr: "code",
        columnHeader: "Code",
        editable: true,
    },
    {
        dataAttr: "version",
        columnHeader: "Version",
        editable: true,
    },
    {
        dataAttr: "system",
        columnHeader: "System",
        editable: true,
    },
];
/* 

  all of this is to support a legend on the page that has been removed from MVP

  this can be added back once we have resources available to make this dynamic

  const legendItems: LegendItem[] = [
      { label: "Name", value: valueSetName },
      { label: "Version", value: "2.5.1" },
      { label: "System", value: "HL7" },
      {
          label: "Reference",
          value: "HL7 guidance for ethnicity (Make this linkable)",
      },
  ];
  
  // currently a placeholder based on design doc
  // TODO: does this need to be more dynamic than we've made it here?
  const ValueSetsDetailHeader = ({
      valueSetName,
      updatedAt,
      updatedBy,
  }: {
      valueSetName: string;
      updatedAt: Date;
      updatedBy: string;
  }) => {
      return (
          <>
              <h1>{valueSetName}</h1>
              <p>
                  File will fail if numeric values or test values are not entered
                  using accepted values or field is left blank.
              </p>
              <p>
                  Accepted values come from values mapped to LOINC codes you can
                  find in the PHN VADS system (needs link).
              </p>
              <p>
                  <b>Last update:</b> {updatedAt.toString()}
              </p>
              <p>
                  <b>Updated by:</b> {updatedBy}
              </p>
          </>
      );
  };

*/

const ValueSetsDetailTable = ({ valueSetName }: { valueSetName: string }) => {
    const valueSetRowArray = useValueSetsRowTable(valueSetName);

    const tableConfig: TableConfig = {
        columns: valueSetDetailColumnConfig,
        rows: valueSetRowArray,
    };
    /* We make this action do what we need it to to add an item */
    const datasetActionItem: DatasetAction = {
        label: "Add item",
    };
    return (
        <Table
            title="ReportStream Core Values"
            datasetAction={datasetActionItem}
            config={tableConfig}
            enableEditableRows
            editableCallback={(row) => {
                console.log("!!! saving row", row);
                return Promise.resolve();
            }}
        />
    );
};

const ValueSetsDetail = () => {
    const { valueSetName } = useParams<{ valueSetName: string }>();
    // TODO: fetch the value set from the API

    return (
        <>
            <Helmet>
                <title>{`Value Sets | Admin | ${valueSetName}`}</title>
            </Helmet>
            <section className="grid-container">
                {/* valueSetsDetailHeader would go here */}
                <h1>{toHumanReadable(valueSetName)}</h1>
                <ValueSetsDetailTable valueSetName={valueSetName} />
            </section>
        </>
    );
};

export default ValueSetsDetail;