/*
 *  This file is part of nzyme.
 *
 *  nzyme is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  nzyme is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with nzyme.  If not, see <http://www.gnu.org/licenses/>.
 */

package horse.wtf.nzyme.dot11.parsers;

import com.codahale.metrics.MetricRegistry;
import horse.wtf.nzyme.dot11.Dot11ManagementFrame;
import horse.wtf.nzyme.dot11.Dot11MetaInformation;
import horse.wtf.nzyme.dot11.MalformedFrameException;
import horse.wtf.nzyme.dot11.frames.Dot11AuthenticationFrame;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.util.ByteArrays;

import java.nio.ByteOrder;

public class Dot11AuthenticationFrameParser extends Dot11FrameParser<Dot11AuthenticationFrame> {

    private final static int MAC_HEADER_LEN = 24;

    private final static int ALGO_NUM_LENGTH = 2;
    private final static int ALGO_NUM_POSITION = MAC_HEADER_LEN;

    private final static int TRANSACTION_SEQ_NO_LENGTH = 2;
    private final static int TRANSACTION_SEQ_NO_POSITION = MAC_HEADER_LEN + 2;

    private final static int STATUS_CODE_LENGTH = 2;
    private final static int STATUS_CODE_POSITION = MAC_HEADER_LEN + 4;

    public Dot11AuthenticationFrameParser(MetricRegistry metrics) {
        super(metrics);
    }

    public enum ALGORITHM_TYPE {
        OPEN_SYSTEM, SHARED_KEY
    }

    @Override
    protected Dot11AuthenticationFrame doParse(byte[] payload, byte[] header, Dot11MetaInformation meta) throws IllegalRawDataException, MalformedFrameException {
        Dot11ManagementFrame auth = Dot11ManagementFrame.newPacket(payload, 0, payload.length);

        try {
            ByteArrays.validateBounds(payload, ALGO_NUM_POSITION, ALGO_NUM_LENGTH);
            ByteArrays.validateBounds(payload, TRANSACTION_SEQ_NO_POSITION, TRANSACTION_SEQ_NO_LENGTH);
            ByteArrays.validateBounds(payload, STATUS_CODE_POSITION, STATUS_CODE_LENGTH);
        } catch(Exception e){
            throw new MalformedFrameException("Payload out of bounds. (1) Ignoring.");
        }

        byte[] algoNumArray = ByteArrays.getSubArray(payload, ALGO_NUM_POSITION, ALGO_NUM_LENGTH);
        byte[] transactionSeqArray = ByteArrays.getSubArray(payload, TRANSACTION_SEQ_NO_POSITION, TRANSACTION_SEQ_NO_LENGTH);
        byte[] statusCodeArray = ByteArrays.getSubArray(payload, STATUS_CODE_POSITION, STATUS_CODE_LENGTH);

        short algorithmCode = ByteArrays.getShort(algoNumArray, 0, ByteOrder.LITTLE_ENDIAN);
        ALGORITHM_TYPE algorithm;
        switch(algorithmCode) {
            case 0:
                algorithm = ALGORITHM_TYPE.OPEN_SYSTEM;
                break;
            case 1:
                algorithm = ALGORITHM_TYPE.SHARED_KEY;
                break;
            default:
                throw new MalformedFrameException("Invalid algorithm type with code [" + algorithmCode + "]. Skipping.");
        }

        short statusCode = ByteArrays.getShort(statusCodeArray, 0, ByteOrder.LITTLE_ENDIAN);
        String status;
        switch(statusCode) {
            case 0:
                status = "SUCCESS";
                break;
            case 1:
                status = "FAILURE";
                break;
            default:
                status = "Invalid/Unknown (" + statusCode + ")";
                break;
        }

        short transactionSequence = ByteArrays.getShort(transactionSeqArray, 0, ByteOrder.LITTLE_ENDIAN);

        String destination = "";
        if (auth.getHeader().getAddress1() != null) {
            destination = auth.getHeader().getAddress1().toString();
        }

        String transmitter = "";
        if (auth.getHeader().getAddress2() != null) {
            transmitter = auth.getHeader().getAddress2().toString();
        }

        return Dot11AuthenticationFrame.create(algorithm, statusCode, status, transactionSequence, destination, transmitter, meta);
    }

}