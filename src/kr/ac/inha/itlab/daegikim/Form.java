package kr.ac.inha.itlab.daegikim;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

public class Form extends JFrame implements ActionListener,Runnable {
    private volatile boolean isStop = false;
    private boolean isEmpty = false;
    private boolean isDebug = true;
    private JButton btnRefresh = null;
    private JButton btnStopOrResume = null;
    private JPanel panSouth = null;
    private JTable table = null;
    private Connection conn = null;

    public Form()
    {
        super("RQViewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        if(!isDebug){
            String hostname = JOptionPane.showInputDialog(null,"Enter your host name: ", "", 1);
            String database = JOptionPane.showInputDialog(null,"Enter your database name: ", "", 1);
            String username = JOptionPane.showInputDialog(null,"Enter your user name: ", "", 1);
            String password = JOptionPane.showInputDialog(null,"Enter your password: ", "", 1);
            conn = getConnection("jdbc:sqlserver://"+hostname+":1433;databaseName="+database+";user="+username+";password="+password+";");
        }
        else{
            conn = getConnection("");
        }

        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        btnRefresh = new JButton("Refresh");
        btnStopOrResume = new JButton("Stop");
        panSouth = new JPanel();
        table = new JTable();

        panSouth.add(setLookAndFeel(btnRefresh));
        panSouth.add(setLookAndFeel(btnStopOrResume));

        con.add(setLookAndFeel(panSouth), BorderLayout.PAGE_END);
        con.add(setLookAndFeel(table), BorderLayout.CENTER);
        btnRefresh.addActionListener(this);
        btnStopOrResume.addActionListener(this);

        this.setUndecorated(true);
        this.setOpacity(0.75f);
        this.setAlwaysOnTop(true);
        this.setLocationByPlatform(true);

        setBounds(0,0,1600,120);
        setVisible(true); // make frame visible
        new ComponentMover(this, this);
    }

    /**
     * 데이터베이스 연결 객체 반환하는 함수
     * @return Connection
     */
    public Connection getConnection(String connectionString) {
        Connection conn = null;
        try {
            Class.forName( "com.microsoft.sqlserver.jdbc.SQLServerDriver" );
            conn = DriverManager.getConnection(connectionString);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return conn;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource()==btnRefresh){
            getData();
        }
        else if(e.getSource()== btnStopOrResume){
            isStop = !isStop;
            btnStopOrResume.setText(isStop?"Resume":"Stop");
        }

    }

    public boolean getData(){
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT \n" +
                    "req.session_id,\n" +
                    "con.client_net_address,\n" +
                    "con.last_read,\n" +
                    "con.last_write,\n" +
                    "sqltext.text,\n" +
                    "req.start_time,\n" +
                    "req.status,\n" +
                    "req.command,\n" +
                    "req.cpu_time,\n" +
                    "req.total_elapsed_time\n" +
                    "FROM sys.dm_exec_requests req\n" +
                    "LEFT JOIN sys.dm_exec_connections con ON req.session_id = con.session_id\n" +
                    "CROSS APPLY sys.dm_exec_sql_text(sql_handle) AS sqltext\n" +
                    "WHERE req.session_id > 50 AND req.session_id <> @@spid");
            ResultSet rs = pstmt.executeQuery();

            Container con = this.getContentPane();
            con.remove(table);
            con.remove(table.getTableHeader());

            table = new JTable(buildTableModel(rs));
            table.getTableHeader().setEnabled(false);
            table.setEnabled(false);

            setLookAndFeel(btnRefresh);
            setLookAndFeel(btnStopOrResume);
            con.add(setLookAndFeel(table.getTableHeader()), BorderLayout.PAGE_START);
            con.add(setLookAndFeel(table), BorderLayout.CENTER);
        } catch (SQLException e1) {
            e1.printStackTrace();
            return false;
        }
        return true;
    }

    public JComponent setLookAndFeel(JComponent com){
        com.setFont(new Font("Malgun Gothic", Font.TRUETYPE_FONT|Font.PLAIN, 12));
        com.setBackground(Color.BLACK);
        com.setForeground(Color.ORANGE);
        com.setBorder(BorderFactory.createLineBorder(Color.ORANGE));

        return com;
    }

    public DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        Vector<Vector<String>> data = new Vector<Vector<String>>();

        Vector<String> columnNames = new Vector<String>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }

        while (rs.next()) {
            Vector<String> vector = new Vector<String>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    vector.add(rs.getString(columnIndex));
            }
            data.add(vector);
        }

        if(data.isEmpty()){
            if(isEmpty==false){ Alarm(); }
            isEmpty = true;
        }
        else{
            isEmpty = false;
        }

        return new DefaultTableModel(data, columnNames);
    }

    public void Alarm(){
        try {
            for(int i=0; i<4; i++){
                Thread.sleep(50);
                panSouth.setBackground(Color.ORANGE);
                Thread.sleep(50);
                panSouth.setBackground(Color.BLACK);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void run() {
        while(true){
            while(isStop == false){
                getData();
                System.gc();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }
}
