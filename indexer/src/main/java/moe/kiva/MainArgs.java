package moe.kiva;

import picocli.CommandLine;

@CommandLine.Command(name = "indexer",
  mixinStandardHelpOptions = true,
  descriptionHeading = "%n@|bold,underline Description|@:%n%n",
  parameterListHeading = "%n@|bold,underline Parameters|@:%n",
  optionListHeading = "%n@|bold,underline Options|@:%n",
  showDefaultValues = true)
public class MainArgs {
  @CommandLine.Option(
    names = {"-u", "--url"},
    description = "URL of the video to fetch",
    paramLabel = "<url>",
    required = true
  )
  public String url;

  @CommandLine.Option(
    names = {"-i", "--id"},
    description = "ID of the video",
    paramLabel = "<integer>",
    required = true
  )
  public int id;

  @CommandLine.Option(
    names = {"-c", "--cat-id"},
    description = "Category ID of the video",
    paramLabel = "<integer>",
    required = true
  )
  public int catId;

  @CommandLine.Option(
    names = {"-n", "--cat-name"},
    description = "Category name of the video",
    required = true
  )
  public String catName;

  @CommandLine.Option(
    names = {"-f", "--flip"},
    description = "Whether to flip the video",
    defaultValue = "false",
    arity = "1",
    paramLabel = "<true|false>"
  )
  public boolean flip;

  @CommandLine.Option(
    names = {"-v", "--volume"},
    description = "Volume of the video",
    defaultValue = "0.35",
    paramLabel = "<float>"
  )
  public float volume;

  @CommandLine.Option(
    names = {"-t", "--title-override"},
    description = "Title override of the video"
  )
  public String titleOverride;

  @CommandLine.Option(
    names = {"-s", "--start-override"},
    description = "Start override of the video",
    paramLabel = "<integer>"
  )
  public Integer startOverride;

  @CommandLine.Option(
    names = {"-e", "--end-override"},
    description = "End override of the video",
    paramLabel = "<integer>"
  )
  public Integer endOverride;
}
