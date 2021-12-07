import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.Date;



public class HttpServer implements Runnable
{
  //static constants
  //HttpServer root is the current directory
  static final File WEB_ROOT = new File(".");
  static final String DEFAULT_FILE = "index.html";
  static final int PORT = 8080; //default port

  //static variables
  static boolean verbose=false;

  //instance variables
  Socket connect;


  //constructor
  public HttpServer(Socket connect)
  {
    this.connect = connect;
  }


  /**
   * main method creates a new HttpServer instance for each
   * request and starts it running in a separate thread
   */
  public static void main(String[] args)
  {
    ServerSocket serverConnect=null;

    if (args.length > 0)
    {
      if (args[0].equals("-v") || args[0].equals("-verbose"))
      {
        verbose = true; //print status to standard out
      }
      else if (args[0].equals("-?") || args[0].equals("-help"))
      {
        //print instructions to standard out
        String instructions =
          "usage: java HttpServer [-options]\n\n" +
          "where options include:\n" +
          "    -? -help\t print out this message\n" +
          "    -v -verbose\t turn on verbose mode";

        System.out.println(instructions);
        return;
      }
    }

    try
    {
      serverConnect = new ServerSocket(PORT); //listen on port
      System.out.println("\nListening for connections on port "
        + PORT + "...\n");
      while (true) //listen until user halts execution
      {
        HttpServer server = new HttpServer(
          serverConnect.accept()); //instantiate HttpServer
        if (verbose)
        {
          System.out.println("Connection opened. (" +
            new Date() + ")");
        }
        //create new thread
        Thread threadRunner = new Thread(server);
        threadRunner.start(); //start thread
      }
    }
    catch (IOException e)
    {
      System.err.println("Server error: " + e);
    }
  }


  /**
   * run method services each request in a separate thread
   */
  public void run()
  {
    try
    {
      //get character input stream from client
      BufferedReader in = new BufferedReader(new
        InputStreamReader(connect.getInputStream()));
      //get character output stream to client (for headers)
      PrintWriter out = new PrintWriter(
        connect.getOutputStream());
      //get binary output stream to client (for requested data)
      BufferedOutputStream dataOut = new BufferedOutputStream(
        connect.getOutputStream());

      //get first line of request from client
      String input = in.readLine();
      //create StringTokenizer to parse request
      StringTokenizer parse = new StringTokenizer(input);
      //parse out method
      String method = parse.nextToken().toUpperCase();
      //parse out file requested
      String fileRequested = parse.nextToken().toLowerCase();

      //methods other than GET and HEAD are not implemented
      if (!method.equals("GET") && !method.equals("HEAD"))
      {
        if (verbose)
        {
          System.out.println("501 Not Implemented: " + method +
            " method.");
        }

        //send Not Implemented message to client
        out.println("HTTP/1.0 501 Not Implemented");
        out.println("Server: HttpServer 1.0");
        out.println("Date: " + new Date());
        out.println("Content-Type: text/html");
        out.println(); //blank line between headers and content
        out.println("<HTML>");
        out.println("<HEAD><TITLE>Not Implemented</TITLE>" +
          "</HEAD>");
        out.println("<BODY>");
        out.println("<H2>501 Not Implemented: " + method +
          " method.</H2>");
        out.println("</BODY></HTML>");
        out.flush();
        out.close(); //close output stream
        connect.close(); //close socket connection

        if (verbose)
        {
          System.out.println("Connection closed.\n");
        }

        return;
      }

      //If we get to here, request method is GET or HEAD

      if (fileRequested.endsWith("/"))
      {
        //append default file name to request
        fileRequested += DEFAULT_FILE;
      }

      try
      {
        //create file object
        File file = new File(WEB_ROOT, fileRequested);
        //get length of file
        int fileLength = (int)file.length();

        //get the file's MIME content type
        String content = getContentType(fileRequested);

        //generate HTTP headers
        out.println("HTTP/1.0 200 OK");
        out.println("Server: HttpServer 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + file.length());
        out.println(); //blank line between headers and content
        out.flush(); //flush character output stream buffer

        //if request is a GET, send the file content
        if (method.equals("GET"))
        {
          //open input stream from file
          FileInputStream fileIn = new FileInputStream(file);
          //create byte array to store file data
          byte[] fileData = new byte[fileLength];
          //read file into byte array
          fileIn.read(fileData);
          fileIn.close(); //close file input stream

          dataOut.write(fileData,0,fileLength); //write file
          dataOut.flush(); //flush binary output stream buffer
        }

        if (verbose)
        {
          System.out.println("File " + fileRequested +
            " of type " + content + " returned.");
        }

        out.close(); //close character output stream
        dataOut.close(); //close binary output stream
        connect.close(); //close socket connection
        if (verbose)
        {
          System.out.println("Connection closed.\n");
        }
      }
      catch (IOException e)
      {
        //inform client file doesn't exist
        fileNotFound(out, fileRequested);

        out.close();
        connect.close();
        if (verbose)
        {
          System.out.println("Connection closed.\n");
        }
      }
    }
    catch (IOException e)
    {
      System.err.println("Server Error: " + e);
    }
  }


  /**
   * fileNotFound informs client that requested file does not
   * exist.
   *
   */
  private void fileNotFound(PrintWriter out, String file)
    throws IOException
  {
    out.println("HTTP/1.0 404 File Not Found");
    out.println("Server: HttpServer 1.0");
    out.println("Date: " + new Date());
    out.println("Content-Type: text/html");
    out.println();
    out.println("<HTML>");
    out.println("<HEAD><TITLE>File Not Found</TITLE>" +
      "</HEAD>");
    out.println("<BODY>");
    out.println("<H2>404 File Not Found: " + file + "</H2>");
    out.println("</BODY>");
    out.println("</HTML>");
    if (verbose)
    {
      System.out.println("404 File Not Found: " + file);
    }
  }


  /**
   * getContentType returns the proper MIME content type
   * according to the requested file's extension
   *
   */
  private String getContentType(String fileRequested)
  {
    if (fileRequested.endsWith(".htm") ||
      fileRequested.endsWith(".html"))
    {
      return "text/html";
    }
    else if (fileRequested.endsWith(".gif"))
    {
      return "image/gif";
    }
    else if (fileRequested.endsWith(".jpg") ||
      fileRequested.endsWith(".jpeg"))
    {
      return "image/jpeg";
    }
    else if (fileRequested.endsWith(".class") ||
      fileRequested.endsWith(".jar"))
    {
      return "applicaton/octet-stream";
    }
    else
    {
      return "text/plain";
    }
  }
} 

