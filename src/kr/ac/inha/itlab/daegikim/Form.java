package kr.ac.inha.itlab.daegikim;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Random;
import java.util.Vector;

public class Form extends JFrame implements ActionListener,Runnable {
    private volatile boolean isStop = false;
    private boolean isDebug = true;
    private JButton btnRefresh = null;
    private JButton btnStopOrResume = null;
    private JPanel panSouth = null;
    private JTable table = null;
    private Connection conn = null;
    private int r = 0;
    private int g = 0;
    private int b = 0;

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

        setBounds(0,0,1600,180);
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
                    "\treq.session_id,\n" +
                    "\treq.request_id,\n" +
                    "\tsqltext.text,\n" +
                    "\treq.start_time,\n" +
                    "\treq.status,\n" +
                    "\treq.command,\n" +
                    "\treq.database_id,\n" +
                    "\treq.user_id,\n" +
                    "\treq.cpu_time,\n" +
                    "\treq.total_elapsed_time\n" +
                    "FROM sys.dm_exec_requests req\n" +
                    "CROSS APPLY sys.dm_exec_sql_text(sql_handle) AS sqltext\n" +
                    "WHERE session_id > 50 AND session_id <> @@spid");
            ResultSet rs = pstmt.executeQuery();

            Container con = this.getContentPane();
            con.remove(table);
            con.remove(table.getTableHeader());

            table = new JTable(buildTableModel(rs));

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
        if(com.equals(table.getTableHeader())||com.equals(btnRefresh)||com.equals(btnStopOrResume)){
            com.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        }
        else{
            com.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        }

        com.setBackground(Color.BLACK);
        com.setForeground(new Color(r,g,b));
        com.setBorder(BorderFactory.createLineBorder(new Color(r,g,b)));


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
        return new DefaultTableModel(data, columnNames);
    }

    @Override
    public void run() {
        while(true){
            while(isStop == false){
                r = new Random().nextInt(255)+1;
                g = new Random().nextInt(255)+1;
                b = new Random().nextInt(255)+1;
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