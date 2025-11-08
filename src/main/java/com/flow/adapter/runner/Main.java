package com.flow.adapter.runner;
import com.flow.adapter.ScanCommand;
import picocli.CommandLine; import picocli.CommandLine.Command;
import java.util.concurrent.Callable;
@Command(name="flow-adapter", mixinStandardHelpOptions=true, version="0.3.0", subcommands={
    ScanCommand.class})
public class Main implements Callable<Integer>{
  public static void main(String[] a){int e=new CommandLine(new Main()).execute(a);System.exit(e);}
  public Integer call(){CommandLine.usage(this,System.out);return 0;}
}