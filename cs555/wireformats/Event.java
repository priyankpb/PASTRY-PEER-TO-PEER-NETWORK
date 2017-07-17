package cs555.wireformats;

public abstract interface Event
{
  public abstract byte[] getByte()
    throws Exception;
  
  public abstract byte getType();
}