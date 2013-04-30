
DROP FUNCTION IF EXISTS "HS_Union"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_SucceedsOrMeets"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_Succeeds"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_PrecedesOrMeets"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_Precedes"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_Overlaps"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_MonthInterval"(timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_Meets"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_Intersect"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_GetTransactionTimestamp"();
DROP FUNCTION IF EXISTS "HS_GetPrimaryKeys"(character varying);
DROP FUNCTION IF EXISTS "HS_GetHistoryRowSetIdentifierColumns"(character varying, character varying[]);
DROP FUNCTION IF EXISTS "HS_ExtractTableIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_ExtractSchemaIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_ExtractColumnIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_ExtractCatalogIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_Except"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_Equals"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_DayInterval"(timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_Contains"(timestamp with time zone, timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_Contains"(timestamp with time zone, timestamp with time zone, timestamp with time zone);
DROP FUNCTION IF EXISTS "HS_CreateCommaSeparatedIdentifierColumnList"(character varying, character varying[], character varying);
DROP FUNCTION IF EXISTS "HS_CreateCommaSeparatedTrackedColumnAndTypeList"(character varying, character varying[]);
DROP FUNCTION IF EXISTS "HS_CreateCommaSeparatedTrackedColumnList"(character varying[], character varying);
DROP FUNCTION IF EXISTS "HS_CreateCommaSeparatedTrackedColumnLiteralList"(character varying[]);
DROP FUNCTION IF EXISTS "HS_CreateIdentifierColumnSelfJoinAndTestCondition"(character varying[], character varying, character varying, character varying, character varying);
DROP FUNCTION IF EXISTS "HS_ConstructIdentifier"(character varying, character varying, character varying);
DROP FUNCTION IF EXISTS "HS_ConstructColumnIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_ConstructColumnIdentifierLiteral"(character varying);
DROP FUNCTION IF EXISTS "HS_CreateIdentifierColumnSelfJoinCondition"(character varying, character varying[], character varying, character varying);
DROP FUNCTION IF EXISTS "HS_ConstructDelTriggerIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_ConstructInsTriggerIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_ConstructSequenceGeneratorIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_ConstructTableIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_ConstructTableTypeIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_ConstructUpdTriggerIdentifier"(character varying);
DROP FUNCTION IF EXISTS "HS_CreateHistoryTableSequenceNumberGenerator"(character varying);
DROP FUNCTION IF EXISTS "HS_CreateHistoryTable"(character varying, character varying[]);
DROP FUNCTION IF EXISTS "HS_InitializeHistoryTable"(character varying, character varying[]);
DROP FUNCTION IF EXISTS "HS_CreateInsertTrigger"(character varying, character varying[]);
DROP FUNCTION IF EXISTS "HS_CreateUpdateTrigger"(character varying, character varying[]);
DROP FUNCTION IF EXISTS "HS_CreateDeleteTrigger"(character varying, character varying[]);
DROP FUNCTION IF EXISTS "HS_CreateHistory"(character varying, character varying[]);
DROP FUNCTION IF EXISTS "HS_DropHistoryErrorCheck"(character varying);
DROP FUNCTION IF EXISTS "HS_DropHistoryTable"(character varying);
DROP FUNCTION IF EXISTS "HS_DropHistoryTriggers"(character varying);
DROP FUNCTION IF EXISTS "HS_DropHistory"(character varying);
DROP FUNCTION IF EXISTS "HS_CreateHistoryErrorCheck"(character varying, character varying[]);
