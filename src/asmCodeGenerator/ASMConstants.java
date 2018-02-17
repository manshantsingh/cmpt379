package asmCodeGenerator;

public class ASMConstants {
	private final static int RECORD_IDENTIFIER_BYTE_LENGTH = 4;
	private final static int RECORD_STATUS_BYTE_LENGTH = 4;
	private final static int RECORD_HEADER_LENGTH = RECORD_IDENTIFIER_BYTE_LENGTH + RECORD_STATUS_BYTE_LENGTH;



	private final static int STRING_LENGTH_BYTE_LENGTH = 4;

	public final static int STRING_IDENTIFIER_VALUE = 6;
	public final static int STRING_LENGTH_OFFSET = RECORD_HEADER_LENGTH;
	public final static int STRING_HEADER_OFFSET = STRING_LENGTH_OFFSET + STRING_LENGTH_BYTE_LENGTH;



	private final static int ARRAY_SUBTYPE_SIZE_BYTE_LENGTH = 4;
	private final static int ARRAY_LENGTH_BYTE_LENGTH = 4;

	public final static int ARRAY_IDENTIFIER_VALUE = 7;
	public final static int ARRAY_SUBELEMENT_SIZE_OFFSET = RECORD_HEADER_LENGTH;
	public final static int ARRAY_LENGTH_OFFSET = ARRAY_SUBELEMENT_SIZE_OFFSET + ARRAY_SUBTYPE_SIZE_BYTE_LENGTH;
	public final static int ARRAY_HEADER_OFFSET = ARRAY_LENGTH_OFFSET + ARRAY_LENGTH_BYTE_LENGTH;
}

