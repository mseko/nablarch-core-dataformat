package nablarch.core.dataformat.convertor.value;

import static nablarch.core.dataformat.DataFormatTestUtils.createInputStreamFrom;
import static nablarch.test.StringMatcher.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import org.hamcrest.CoreMatchers;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.test.support.tool.Hereis;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link NumberString}の機能結合テスト。
 *
 * @author Masato Inoue
 */
public class NumberStringIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DataRecordFormatter formatter = null;

    /** フォーマッタを生成する。 */
    private void createFormatter(File file) {
        formatter = FormatterFactory.getInstance()
                                    .setCacheLayoutFileDefinition(false)
                                    .createFormatter(file);
    }

    private void createFile(File formatFile, String... lines) throws Exception {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(formatFile), "utf-8"));
        try {
            for (final String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
            writer.flush();
        } finally {
            writer.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (formatter != null) {
            formatter.close();
        }
    }

    /**
     * 入力された文字列をBigDecimal型としてDataRecordに格納できることの確認。
     */

    @Test
    public void testReadNumberStringConvertor() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile,
                "file-type: \"Variable\"",
                "text-encoding: \"ms932\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "quoting-delimiter: \"\\\"\"",
                "",
                "[TestDataRecord]",
                "1  amount1        X  number    # 数値1",
                "2  amount2        X  number    # 数値2",
                "3  amount3        X  number    # 数値3"
        );
        createFormatter(formatFile);

        InputStream inputStream = createInputStreamFrom("\"12.345\",\"234.56\",\"34567\"");
        formatter.setInputStream(inputStream)
                 .initialize();

        DataRecord readRecord = formatter.readRecord();
        assertThat(readRecord.get("amount1"), CoreMatchers.<Object>is(new BigDecimal("12.345")));
        assertThat(readRecord.get("amount2"), CoreMatchers.<Object>is(new BigDecimal("234.56")));
        assertThat(readRecord.get("amount3"), CoreMatchers.<Object>is(new BigDecimal("34567")));
    }

    /**
     * NumberStringConvertorを使用し、
     * DataRecordに設定された符号なし数値、Integer型の数値、BigDecimal型の数値が正常にファイルに出力されることの確認。
     * (DBから取得したデータを書き出す場合の想定）
     */
    @Test
    public void testWriteNumberStringConvertor() throws Exception {
        final File formatFile = temporaryFolder.newFile("format.dat");
        createFile(formatFile,
                "file-type: \"Variable\"",
                "text-encoding: \"ms932\"",
                "record-separator: \"\\n\"",
                "field-separator: \",\"",
                "quoting-delimiter: \"\\\"\"",
                "",
                "[TestDataRecord]",
                "1  amount1        X  number    # 数値1",
                "2  amount2        X  number    # 数値2",
                "3  amount3        X  number    # 数値3"
        );
        createFormatter(formatFile);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.setOutputStream(outputStream)
                 .initialize();

        formatter.writeRecord(new DataRecord() {{
            put("amount1", "12.345"); // String Number
            put("amount2", 234.56); // Integer
            put("amount3", new BigDecimal(34567)); // BigDecimal
        }});

        assertThat(outputStream.toString("ms932"), is("\"12.345\",\"234.56\",\"34567\"\n"));
    }


    /**
     * 入力時にエラーが発生し、例外メッセージにレコード番号およびフィールド名が付与されることのテスト。
     */
    @Test
    public void testExceptionConfirmMessage() throws Exception {
        File formatFile = Hereis.file("./format.dat");
        /***********************************************************
         file-type:         "Variable"
         text-encoding:     "ms932" # 文字列型フィールドの文字エンコーディング
         record-separator:  "\n"       # レコード区切り文字
         field-separator:   ","       # フィールド区切り文字
         quoting-delimiter: "\""       # クオート文字

         [TestDataRecord]
         1  amount1        X  number    # 数値1
         2  amount2        X  number    # 数値2
         3  amount3        X  number    # 数値3
         ************************************************************/
        formatFile.deleteOnExit();

        createFormatter(formatFile);

        InputStream inputStream = createInputStreamFrom("\"a\",\"234.56\",\"34567\"");
        formatter.setInputStream(inputStream)
                 .initialize();

        try {
            formatter.readRecord();
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(), startsWith(
                    "invalid parameter format was specified. parameter format must be [^([0-9][0-9]*)?[0-9](\\.[0-9]*[0-9])?$]. value=[a]. "));
            assertThat(e.getFieldName(), is("amount1"));
            assertThat(e.getRecordNumber(), is(1));
        }
    }
}
