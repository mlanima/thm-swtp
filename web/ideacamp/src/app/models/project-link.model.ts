import { z } from "zod";


export const ProjectLinkSchema = z.object({
  id : z.string().uuid(),
  projectId : z.string().uuid(),
  label : z.string().max(100),
  url : z.string().url().max(300),
  createdAt : z.string(),
  updatedAt : z.string(),
});

export type ProjectLinkModel = z.infer<typeof ProjectLinkSchema>;
