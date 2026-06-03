import type { UserNotificationVO } from "@/services/medical/tongzhiguanli";

export function resolveNotificationRoute(
  item: UserNotificationVO,
  role: "user" | "doctor",
): string | null {
  if (item.bizType === "BILL" && role === "user") {
    return "/patient/payments";
  }
  if (item.bizType === "APPOINTMENT") {
    return role === "doctor" ? "/doctor/appointments" : "/patient/my-appointments";
  }
  return null;
}
