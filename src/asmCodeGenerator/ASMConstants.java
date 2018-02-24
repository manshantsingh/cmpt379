package asmCodeGenerator;

public class ASMConstants {
	private final static int RECORD_TYPE_ID_BYTE_LENGTH = 4;
	private final static int RECORD_STATUS_BYTE_LENGTH = 4;
	private final static int RECORD_HEADER_LENGTH = RECORD_TYPE_ID_BYTE_LENGTH + RECORD_STATUS_BYTE_LENGTH;

	public final static int RECORD_TYPE_ID_OFFSET = 0;
	public final static int RECORD_STATUS_OFFSET = RECORD_TYPE_ID_OFFSET + RECORD_TYPE_ID_BYTE_LENGTH;


	private final static int STRING_LENGTH_BYTE_LENGTH = 4;

	public final static int STRING_TYPE_ID= 6;
	public final static int STRING_STATUS = 0b1001;
	public final static int STRING_LENGTH_OFFSET = RECORD_HEADER_LENGTH;
	public final static int STRING_HEADER_OFFSET = STRING_LENGTH_OFFSET + STRING_LENGTH_BYTE_LENGTH;



	private final static int ARRAY_SUBTYPE_SIZE_BYTE_LENGTH = 4;
	private final static int ARRAY_LENGTH_BYTE_LENGTH = 4;

	public final static int ARRAY_TYPE_ID= 7;
	public final static int ARRAY_STATUS_WITHOUT_REFERENCE_SUBTYPE = 0;
	public final static int ARRAY_STATUS_WITH_REFERENCE_SUBTYPE = 0b0010;
	public final static int ARRAY_SUBELEMENT_SIZE_OFFSET = RECORD_HEADER_LENGTH;
	public final static int ARRAY_LENGTH_OFFSET = ARRAY_SUBELEMENT_SIZE_OFFSET + ARRAY_SUBTYPE_SIZE_BYTE_LENGTH;
	public final static int ARRAY_HEADER_OFFSET = ARRAY_LENGTH_OFFSET + ARRAY_LENGTH_BYTE_LENGTH;


	public final static int MASK_RECORD_CHECK_ALLOWS_DELETION = 0b1100;
	public final static int MASK_RECORD_SET_IS_DELETED = 0b0100;
	public final static int MASK_ARRAY_CHECK_REFERENCE_SUBTYPE = 0b0010;
}

